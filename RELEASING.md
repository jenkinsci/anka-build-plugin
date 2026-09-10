# Releasing the Anka Build Plugin

1. Make changes in release/vX.X.X branch
2. PR into main branch (so Jenkins' CI runs)
3. Check the PR changes page and look for missing test coverage, warnings, etc, and fix them
4. Check the PR CI and be sure it's all green
5. Update the version in the pom.xml file to X.X.X-SNAPSHOT (X.X.X is the version you're releasing)
6. Run https://VEERTUJENKINS/view/jenkins/job/plugin-jenkins-build/ (auto runs usually)
7. Run https://VEERTUJENKINS/view/jenkins/job/cloud-integration-jenkins/ against the new build you just did
8. If it passes, you're ok to release https://VEERTUJENKINS/view/jenkins/job/plugin-jenkins-release
9. After release, download the artifact HPI (in the plugin-jenkins-release job), then merge to main
10. Get the hpi file and create a release in Github manually (tag should already exist)