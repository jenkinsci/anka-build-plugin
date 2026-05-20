# Node Labels HTTP API

This optional feature lets automation update **static node label templates** (`AnkaCloudSlaveTemplate` entries) on an Anka cloud **without** Jenkins UI access or `Overall/Administer` permission. It is intended for external schedulers, pipelines that manage inventory, or Configuration-as-Code–adjacent workflows.

The implementation follows the usual Jenkins pattern: [`UnprotectedRootAction`](https://javadoc.jenkins.io/hudson/model/UnprotectedRootAction.html) plus a **narrow** CSRF [crumb exclusion](https://www.jenkins.io/doc/developer/security/misc/) for this path only, and a **shared secret** checked before any privileged work.

Related issue: [jenkinsci/anka-build-plugin#59](https://github.com/jenkinsci/anka-build-plugin/issues/59).

## Enable the API (per cloud)

1. Open **Manage Jenkins → Nodes and Clouds → Clouds** and edit your **Anka Build Cloud** entry.
2. Set **Labels API token** (a strong random secret).  
   - **Empty** = the Labels API is **disabled** for that cloud (`503` on use).
3. Save the configuration.

Treat the token like a password: store it in a secret manager, rotate it from the UI when needed, and **use HTTPS** in production so the token is not sent in cleartext.

## Endpoint

| Item | Value |
|------|--------|
| Method | **POST** only (other methods → `405`) |
| Path | `{JENKINS_URL}/anka-build-cloud/labels/{cloudName}` |
| `cloudName` | Must match the Jenkins cloud name **exactly**. URL-encode spaces and special characters (e.g. `Veertu%20anka`). |
| Content-Type | `application/json` |

There is **no** GET handler; this surface is intentionally minimal.

## Authentication

Send **one** of:

- `Authorization: Bearer <token>`
- `X-Anka-Labels-Token: <token>`

If the token is missing or wrong → **401**.  
If the cloud has no token configured → **503**.

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

Each element is deserialized the same way as the cloud UI and **Configuration as Code**: Stapler/DataBound binding into `AnkaCloudSlaveTemplate`. In practice, use the same field names as in your CasC YAML under `jenkins.clouds[].ankaMgmt.templates[]`.

Reference copy in this repo: [`src/test/resources/com/veertu/plugin/anka/configuration-as-code.yml`](../src/test/resources/com/veertu/plugin/anka/configuration-as-code.yml).

Validation rules:

- **`label`** — required, non-empty.
- **`masterVmId`** — required, non-empty.
- **`cloudName`** — if omitted, it is set to the target cloud’s name.
- **Duplicate `label` values** inside the **same request** → **400**.

If the controller can reach the Anka Build Cloud and `masterVmId` is not known there, the API **still succeeds** but may **log a warning** (soft check) so temporary controller or registry issues do not block updates.

## Responses

| Status | Meaning |
|--------|---------|
| **200** | Success. Body is JSON: `cloudName`, `mode`, `previousCount`, `newCount`. |
| **400** | Invalid JSON, bad `mode`, missing `templates`, binding/validation errors (e.g. duplicate labels in payload, missing `label` / `masterVmId`). |
| **401** | Missing or invalid token. |
| **404** | Unknown cloud name or not an Anka Build Cloud. |
| **405** | Method other than POST. |
| **503** | Labels API token not set for that cloud. |
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
- **Persistence** — Updates replace the cloud entry in Jenkins configuration and save; a restart is not required for the new templates to apply to provisioning.
- **Concurrency** — Two simultaneous POSTs can race; the last successful `save()` wins. Serialize updates per cloud if this matters for your automation.
- **CasC export** — After you add a Labels API token in the UI, it appears in exported Configuration-as-Code like other secrets (typically redacted); verify your export pipeline if you rely on YAML reviews.

## Troubleshooting

| Symptom | Things to check |
|---------|------------------|
| `404` | Cloud name spelling and URL encoding; cloud must be `AnkaMgmtCloud`. |
| `401` | Header name/value; no extra quotes; Bearer spelling. |
| `503` | Token field empty in cloud config; save configuration after setting. |
| `400` on templates | Match CasC/UI shape; ensure nested objects (`retentionStrategy`, `saveImageParameters`, etc.) match what the UI would save; see `configuration-as-code.yml` in this repo. |
| Changes not reflected | Confirm HTTP `200` and `newCount`; check Jenkins system log for warnings about `masterVmId`. |
