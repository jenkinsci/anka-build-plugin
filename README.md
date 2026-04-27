# Anka Jenkins Build Plugin

[![PR Maven Tests](https://github.com/jenkinsci/anka-build-plugin/actions/workflows/pr-maven-tests.yml/badge.svg)](https://github.com/jenkinsci/anka-build-plugin/actions/workflows/pr-maven-tests.yml)

Usage of this plugin is detailed on our [Official Documentation](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/jenkins/).

This plugin integrates with the [Anka Build Cloud](https://ankadocs.veertu.com/docs/anka-build-cloud/) and allows on-demand provisioning of Anka VMs for your Jenkins jobs.

Contributors are welcome! Please submit an Issue or PR.

---

## Dependency Requirements

The Anka Build plugin requires the following Jenkins plugin dependencies at the
minimum versions listed below. If you install the `.hpi` manually rather than
through the Jenkins Update Center, you MUST upgrade these dependencies first.

| Jenkins plugin               | Minimum version                          |
| ---------------------------- | ---------------------------------------- |
| `plain-credentials`          | `199.v9f8e1f741799`                      |
| `bouncycastle-api`           | `2.30.1.80-261.v00c0e2618ec3`            |
| `workflow-basic-steps`       | `1079.vce64b_a_929c5a_`                  |
| `workflow-durable-task-step` | `1434.v1b_595c29ddd7`                    |
| `ssh-slaves`                 | `3.1031.v72c6b_883b_869`                 |
| `node-iterator-api`          | `72.vc90e81737df1`                       |

Starting with v2.15.x, if any of these requirements are not met the Anka Build
plugin will **refuse to start Jenkins** (a fatal initializer runs before
`SYSTEM_CONFIG_LOADED`). This intentional halt protects the Anka cloud
configuration stored in `$JENKINS_HOME/config.xml` from being silently wiped
when Jenkins boots without the classes needed to deserialize
`com.veertu.plugin.anka.AnkaMgmtCloud` entries. Upgrade the dependencies listed
above and restart Jenkins to recover — your `config.xml` is left untouched on
disk during the halt.

---

## Development

### Dependency floors

The `<version>` floors declared for Jenkins plugin deps in `pom.xml` are
intentionally kept low (LTS 2.440-era) and marked `<optional>true</optional>`.
Do NOT raise them to match the BOM-resolved versions. Low floors let
`anka-build` load even on older Jenkins installs; the real required minimums
are enforced at runtime by `DependencyVerifier`, which reads them from the
`required.<shortName>.version` properties in `pom.xml` (via the filtered
`src/main/resources/com/veertu/plugin/anka/required-dep-versions.properties`)
and halts Jenkins startup before `SYSTEM_CONFIG_LOADED` if they are not met.
Raising the pom floors reopens the bug where Jenkins silently drops
`<clouds>` entries from `config.xml` when the plugin fails its manifest
dependency check. When bumping `jenkins.baseline` or the BOM, update the
`required.*.version` properties in `pom.xml` to match — the
`verify-required-dep-versions-in-sync` build step enforces that every
`<dependency>` has a matching property.

### Build

Build locally with Maven:

```bash
mvn -Daether.remoteRepositoryFilter.prefixes=false clean package
```

If you use Maven 3, you can usually omit `-Daether.remoteRepositoryFilter.prefixes=false`.

Or, alternatively, build in Docker:

```bash
./build-docker.bash
```

### Test

Run all tests:

```bash
mvn -Daether.remoteRepositoryFilter.prefixes=false test
```

Run a single test class:

```bash
mvn -Daether.remoteRepositoryFilter.prefixes=false -Dtest=ConfigurationAsCodeTest test
```

## Release

```bash
mvn release:prepare release:perform
```

If needed, rollback the release:

```bash
mvn release:rollback
```
