# Java Path on Node Labels

Node Labels (templates) can set an optional **Java Path**: the path to the Java executable on the Anka VM used when Jenkins installs and launches the remoting agent (`agent.jar`).

This matches the **JavaPath** field on a normal Jenkins SSH agent node. It applies to both **SSH** and **JNLP** launch methods.

Related issue: [jenkinsci/anka-build-plugin#83](https://github.com/jenkinsci/anka-build-plugin/issues/83).

## Default behavior (leave empty)

Leave **Java Path** empty (or omit `javaPath` in Configuration as Code / API) to keep the previous behavior: use whatever `java` the VM resolves from its default `PATH`.

- Existing labels without this field continue to work unchanged.
- Empty string is treated the same as unset (`null`).

## Configure in the Jenkins UI

1. Open **Manage Jenkins → Clouds** and edit your **Anka Build Cloud**.
2. Edit (or create) a **Node Label** / template.
3. Under **Launch Method**, find **Java Path** next to **Java options**.
4. Enter the absolute path to `java` on the Anka VM, or leave blank for the VM `PATH` default.
5. Save.

The same field is available on the **Create Dynamic Anka Node** pipeline step configuration.

### Example paths (macOS Anka VMs)

Anka Build Cloud VMs are macOS. Examples:

| Layout | Example |
| ------ | ------- |
| Homebrew OpenJDK 21 (Apple Silicon) | `/opt/homebrew/opt/openjdk@21/bin/java` |
| Temurin / JDK under `/Library` | `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java` |
| Xcode CLT-style `java_home` result | path from `$(/usr/libexec/java_home -v 21)/bin/java` inside the VM |

The path must exist on the guest macOS VM (the Anka image your label uses). Do not use a Java path from the Jenkins controller machine.

## Configuration as Code

Set `javaPath` on a template:

```yaml
jenkins:
  clouds:
    - ankaMgmt:
        cloudName: "Veertu anka"
        # ...
        templates:
          - cloudName: "Veertu anka"
            label: "macos-jdk21"
            launchMethod: "ssh"
            credentialsId: "anka-creds"
            masterVmId: "xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            remoteFS: "/Users/anka"
            javaPath: "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"
            # omit javaPath (or leave "") to use java from the VM PATH
```

## Labels HTTP API

When updating templates via the [Node Labels HTTP API](node-labels-api.md), include `javaPath` on the template JSON object the same way as other template fields (for example `javaArgs`, `launchMethod`). Omit it or send `null` / `""` for the VM `PATH` default.

## Pipeline dynamic node step

On **Create Dynamic Anka Node**, set **Java Path** in the step UI, or bind `javaPath` when configuring the step in Pipeline / Job DSL the same way as other template properties (alongside `javaArgs`).

## How it is used

| Launch method | Behavior when set | Behavior when unset / blank |
| ------------- | ----------------- | --------------------------- |
| **SSH** | Passed to Jenkins `SSHLauncher` as Java Path | `null` → SSH launcher uses `java` on the agent `PATH` |
| **JNLP** | Startup script runs the configured binary | Script uses bare `java` (VM `PATH`) |

**Java options** (`javaArgs`) remains separate: JVM flags such as `-Xmx512m`. **Java Path** selects which Java binary runs those options.
