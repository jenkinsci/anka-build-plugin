# Anka Build Plugin

[![PR Maven Tests](https://github.com/jenkinsci/anka-build-plugin/actions/workflows/pr-maven-tests.yml/badge.svg)](https://github.com/jenkinsci/anka-build-plugin/actions/workflows/pr-maven-tests.yml)

Usage of this plugin is detailed on our [Official Documentation](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/jenkins/).

This plugin integrates with the [Anka Build Cloud](https://ankadocs.veertu.com/docs/anka-build-cloud/) and allows on-demand provisioning of Anka VMs for your Jenkins jobs.

Contributors are welcome! Please submit an Issue or PR.

---

## Development

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
