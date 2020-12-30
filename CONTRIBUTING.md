# Thanks for your interest in contributing!

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) prior to contributing.

# Expectations
The expected time frame for a response to a comment/change/update/commit/etc is 3 working days for contributors and maintainers alike.

If at any time, you feel uncertain as to the status of an open issue/PR, please do one of the following:
- comment and mention current maintainers
- contact us through our [Slack](https://veertuchat.slack.com)
- [email our support](mailto:support@veertu.com)

# Using Issues

We welcome Bug Reports and Feature Requests in the form of Issues. However, we cannot guarantee the speed at which new features or bugs are resolved. We highly recommend creating a PR with the necessary code changes if you require an expedited turn around.

Please submit a thorough Issue description. The clearer and more details we have to understand the issue, the sooner we can prioritize the request for a release.

Labels will be used/set by maintainers throughout the life-cycle of the Issue. Check out our [Label Reference](#labeling-reference) to understand which labels are used.

# Submitting Pull Requests

### Guidelines for Contributors
* Branch off an updated version of `master`
* Do not integrate cosmetic/style/format changes. [Why?](#cosmetic-changes)
* Keep PRs small and specific
* Follow the existing code style and patterns
* Do not update dependencies unless it fixes an issue

Once a PR is submitted, maintainers will determine that the PR will provide a viable improvement for the project.

Once the PR is approved by a maintainer, it will be put into a "code freeze" state, which usually means we are running our internal tests on it.

After it has been cleared, it will be (squash) merged to a release branch and labeled accordingly.

Labels will be used/set by maintainers throughout the life-cycle of the Issue. Check out our [Label Reference](#labeling-reference) to understand which labels are used.

### Cosmetic Changes
We do not accept PRs that include cosmetic/format/style changes.
- Maintainers are expected to review PRs thoroughly. Time spent reviewing complex or lots of small useless code lines can instead be put into bug fixes or introducing new functionality.
- It complicates our ability to investigate how bugs were created.
- Hinders maintainer backporting work.

# Labeling Reference

## Global Labels
Label | Description
------| ----------------
rending review | Set when a maintainer review is needed
missing information | More information/clarification is required
won't do | Maintainers have decided to not integrate this in the project
pending release | This will be included in a release version
X.X.X | The version that will include this

## Issue Specific Labels
Label | Description
------| ----------------
in backlog | Issue has been added to Veertu's backlog
help wanted | Maintainers are not able to immediately get to working on the issue and would love for contributors to submit a PR
resolved | Issue has been resolved and released

## PR Specific Labels
Label | Description
------| ----------------
in progress | Maintainer and/or contributor are actively working on PR
code freeze | Avoid code changes (usually due to internal testing)
changes required | Updates to the PR are required
released | The PR has been released as part of a version