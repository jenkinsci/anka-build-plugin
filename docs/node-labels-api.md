# Node Labels HTTP API

This optional feature lets automation update **static node label templates** (`AnkaCloudSlaveTemplate` entries) on an Anka cloud **without** Jenkins UI access or `Overall/Administer` permission. It is intended for external schedulers, pipelines that manage inventory, or Configuration-as-Code–adjacent workflows.

The implementation follows the usual Jenkins pattern: [`UnprotectedRootAction`](https://javadoc.jenkins.io/hudson/model/UnprotectedRootAction.html) with [`StaplerProxy#getTarget()`](https://javadoc.jenkins.io/component/stapler/org/kohsuke/stapler/StaplerProxy.html) returning {@code null} when the Labels API is not configured (so Stapler does not route to the plugin), a **narrow** CSRF [crumb exclusion](https://www.jenkins.io/doc/developer/security/misc/) for this path only, and a **shared secret** checked before any privileged work.

Related issue: [jenkinsci/anka-build-plugin#59](https://github.com/jenkinsci/anka-build-plugin/issues/59).

## Enable the API (per cloud)

1. Create a Jenkins credential of type **Anka Build Cloud Plugin: Labels API Token** (**Manage Jenkins → Credentials**) with a strong random secret.
2. Open **Manage Jenkins → Nodes and Clouds → Clouds** and edit your **Anka Build Cloud** entry.
3. Under **Security → Label Update API**, select **Labels API token credential** (the credential id from step 1).  
   - **Empty** = the Labels API URL is **not registered** for that cloud ({@code StaplerProxy#getTarget()} returns {@code null}; Stapler does not route requests to the plugin).
4. Save the configuration.

Treat the token like a password: store it in a secret manager, rotate it by updating the credential when needed, and **use HTTPS** in production so the token is not sent in cleartext.

### Configuration as Code

Provision the credential first, then reference its id on the cloud:

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - labelsApiToken:
              id: "anka-labels-api-token"
              scope: GLOBAL
              token: "${LABELS_API_TOKEN}"
              description: "Anka Build Cloud Plugin: Labels API Token"

jenkins:
  clouds:
    - ankaMgmt:
        cloudName: "Veertu anka"
        labelsApiTokenCredentialsId: "anka-labels-api-token"
        # ...
```

## Endpoint

| Item | Value |
|------|--------|
| Method | **POST** only (other methods are rejected, often `404` from Stapler dispatch or `405`) |
| Path | `{JENKINS_URL}/anka-build-cloud/labels/{cloudName}` |
| `cloudName` | Must match the Jenkins cloud name **exactly**. URL-encode spaces and special characters (e.g. `Veertu%20anka`). |
| Content-Type | `application/json` |

There is **no** GET handler; this surface is intentionally minimal.

## Authentication

Send **one** of:

- `Authorization: Bearer <token>`
- `X-Anka-Labels-Token: <token>`

If the token is missing or wrong → **401**.  
If the cloud has no token configured, or no cloud on the controller has a token configured → **404** ({@code getTarget()} returns {@code null}; the request is not routed to the plugin).

## Request body

JSON object with:

| Field | Required | Description |
|-------|----------|-------------|
| `mode` | Yes | `replace` or `append` (case-insensitive). |
| `templates` | Yes | Array of template objects. |

### Modes

- **`replace`** — After a successful call, the cloud’s **static** `templates` list is **exactly** the `templates` array from the request (same order as sent).
- **`append`** — Start from the current list. For each object in the request, if a template with the same **label** already exists, it is **replaced**; otherwise the object is **appended**. Templates **not** mentioned in the request are **unchanged**.

### Template objects

Each element is deserialized the same way as the cloud UI and **Configuration as Code**: Stapler/DataBound binding into `AnkaCloudSlaveTemplate`. In practice, use the same field names as in your CasC YAML under `jenkins.clouds[].ankaMgmt.templates[]`, except **`cloudName`** — do not send it; the API sets it from the URL.

Reference copy in this repo: [`src/test/resources/com/veertu/plugin/anka/configuration-as-code.yml`](../src/test/resources/com/veertu/plugin/anka/configuration-as-code.yml).

Validation rules:

- **Request body** — maximum **1 MiB** (1,048,576 bytes). Larger bodies → **413**.
- **`label`** — required, non-empty.
- **`masterVmId`** — required, non-empty.
- **Duplicate `label` values** inside the **same request** → **400**.

If the controller can reach the Anka Build Cloud and `masterVmId` is not known there, the API **still succeeds** but may **log a warning** (soft check) so temporary controller or registry issues do not block updates.

## Responses

| Status | Meaning |
|--------|---------|
| **200** | Success. Body is JSON: `cloudName`, `mode`, `previousCount`, `newCount`. |
| **400** | Invalid JSON, bad `mode`, missing `templates`, binding/validation errors (e.g. duplicate labels in payload, missing `label` / `masterVmId`). |
| **401** | Missing or invalid token. |
| **404** | URL not registered (no Labels API token on the target cloud, or none configured on any cloud); unknown cloud; non-POST (e.g. GET) when Stapler does not dispatch to a handler. |
| **405** | Method other than POST (when the request is dispatched but verb is wrong). |
| **413** | Request body exceeds 1 MiB. |
| **500** | Unexpected server error while applying changes (check Jenkins logs). |

### Success body example

```json
{
  "cloudName": "Veertu anka",
  "mode": "append",
  "previousCount": 2,
  "newCount": 3
}
```

## `curl` examples

Replace `JENKINS_URL`, `CLOUD_NAME`, and `TOKEN`. Encode `CLOUD_NAME` for the URL if it contains spaces.

**Replace all static templates**

```bash
curl -sS -X POST \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer TOKEN' \
  --data '{
    "mode": "replace",
    "templates": [
      {
        "label": "macos-ci",
        "masterVmId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "tag": "latest",
        "remoteFS": "/Users/anka",
        "launchMethod": "ssh",
        "credentialsId": "agent-ssh",
        "numberOfExecutors": 1,
        "schedulingTimeout": 1800,
        "retentionStrategy": { "ankaRunOnceCloud": { "idleMinutes": 5 } },
        "saveImage": false,
        "saveImageParameters": {
          "saveImage": false,
          "templateID": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
          "tag": "latest",
          "dontAppendTimestamp": false,
          "deleteLatest": false,
          "description": "",
          "suspend": false,
          "waitForBuildToFinish": false
        }
      }
    ]
  }' \
  'JENKINS_URL/anka-build-cloud/labels/CLOUD_NAME'
```

**Append or update by label** (using the alternative header)

```bash
curl -sS -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Anka-Labels-Token: TOKEN' \
  --data '{"mode":"append","templates":[{"label":"macos-ci","masterVmId":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"}]}' \
  'JENKINS_URL/anka-build-cloud/labels/CLOUD_NAME'
```

## CSRF (crumbs)

Automation may POST **without** a Jenkins crumb on this path: only URLs under `/anka-build-cloud/labels/` are excluded. All other Jenkins APIs still require normal CSRF protection.

## Behavior notes

- **Static labels only** — Dynamic/runtime templates are not part of this API.
- **Persistence** — Updates mutate static templates on the registered cloud and call `Jenkins.save()`; a restart is not required for the new templates to apply to provisioning.
- **Concurrency** — Two simultaneous POSTs can race; the last successful `save()` wins. Serialize updates per cloud if this matters for your automation.
- **CasC export** — The cloud stores the credential **id** (`labelsApiTokenCredentialsId`); the token secret lives in the credentials store and is exported separately (typically redacted).

## Troubleshooting

| Symptom | Things to check |
|---------|------------------|
| `404` | Cloud name spelling and URL encoding; cloud must be `AnkaMgmtCloud`. |
| `401` | Header name/value; no extra quotes; Bearer spelling. |
| `404` on a cloud you expect to use | Labels API token credential not selected in cloud config, or credential id does not resolve to a token; save configuration after setting. |
| `400` on templates | Match CasC/UI shape; ensure nested objects (`retentionStrategy`, `saveImageParameters`, etc.) match what the UI would save; see `configuration-as-code.yml` in this repo. |
| Changes not reflected | Confirm HTTP `200` and `newCount`; check Jenkins system log for warnings about `masterVmId`. |
