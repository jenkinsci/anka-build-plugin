# Anka Build Plugin

Usage of this plugin is detailed on our [Official Documentation](https://docs.veertu.com/anka/plugins-and-integrations/controller-+-registry/jenkins/).

This plugin integrates with the [Anka Build Cloud](https://ankadocs.veertu.com/docs/anka-build-cloud/) and allows on-demand provisioning of Anka VMs for your Jenkins jobs.

Contributors are welcome! Please submit an Issue or PR.

---

## Development

### Build

- Install Maven 3 (tested with 3.9.9)
- Install OpenJDK 17

```
export PATH="${HOME}/apache-maven-4.0.0-rc-2/bin:$PATH"
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
mvn package
```
