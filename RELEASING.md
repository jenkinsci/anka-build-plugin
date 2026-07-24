# Releasing the Anka Build Plugin

1. Make changes in release/vX.X.X branch
2. PR into main branch (so Jenkins' CI runs)
3. Update the version in the pom.xml file to X.X.X-SNAPSHOT
4. Run https://VEERTUJENKINS/view/jenkins/job/plugin-jenkins-build/
5. Run https://VEERTUJENKINS/view/jenkins/job/cloud-integration-jenkins/ against the new build you just did
6. Check the PR CI and be sure it's all green
7. If it passes, you're ok to release https://VEERTUJENKINS/view/jenkins/job/plugin-jenkins-release
8. After release, merge to main