# Anka Build Plugin

Usage of this plugin is detailed on our [Official Documentation](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/jenkins/)

This plugin integrates with an [Anka Build Cloud](https://ankadocs.veertu.com/docs/anka-build-cloud/) and allows on-demand provisioning of Anka VMs for your pipeline jobs.

## Build

- Install Maven 4.0.0-rc-2
- Install OpenJDK 17

```
export PATH="${HOME}/apache-maven-4.0.0-rc-2/bin:$PATH"
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
mvn package
```
