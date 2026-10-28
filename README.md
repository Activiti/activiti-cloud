# activiti-cloud

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![CI](https://github.com/Activiti/activiti-cloud/actions/workflows/main.yml/badge.svg)](https://github.com/Activiti/activiti-cloud/actions/workflows/main.yml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/05862b3db7344b24b4509d266df77c3a)](https://www.codacy.com/gh/Activiti/activiti-cloud?utm_source=github.com&utm_medium=referral&utm_content=Activiti/activiti-cloud&utm_campaign=Badge_Grade)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud)](https://cla-assistant.io/Activiti/activiti-cloud)
[![security status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud/security)](https://www.meterian.io/report/gh/Activiti/activiti-cloud)
[![stability status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud/stability)](https://www.meterian.io/report/gh/Activiti/activiti-cloud)
[![licensing status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud/licensing)](https://www.meterian.io/report/gh/Activiti/activiti-cloud)

Activiti Cloud libraries and Spring Boot starters.

## CI/CD

Running on GH Actions.

For Dependabot PRs to be validated by CI, the label "CI" should be added to the PR.

Requires the following secrets to be set:

| Name                         | Description                        |
| ---------------------------- | ---------------------------------- |
| BOT_GITHUB_TOKEN             | Token to launch other builds on GH |
| BOT_GITHUB_USERNAME          | Username to issue propagation PRs  |
| DOCKERHUB_USERNAME           | Docker Hub repository username     |
| DOCKERHUB_PASSWORD           | Docker Hub repository password     |
| NEXUS_USERNAME               | Maven repository username (CI)     |
| NEXUS_PASSWORD               | Maven repository password (CI)     |
| RANCHER2_URL                 | Rancher URL for tests              |
| RANCHER2_ACCESS_KEY          | Rancher access key for tests       |
| RANCHER2_SECRET_KEY          | Rancher secret key for tests       |
| SLACK_NOTIFICATION_BOT_TOKEN | Token to notify slack on failure   |

## Consuming Activiti previews from Cloudsmith

The Cloudsmith consumption trial uses Activiti `0.0.1-PR-5629-SNAPSHOT`, published
by [Activiti PR #5629](https://github.com/Activiti/Activiti/pull/5629). All four
`activiti.version` properties select this version. Keep the trial PR unmerged
while these temporary preview dependencies are selected.

Set the GitHub Actions variable `CLOUDSMITH_NAMESPACE` to `hyland`, either on this
repository or at organization level with access granted to this repository.
The existing `cloudsmith-auth` action derives the `activiti-activiti-cloud` OIDC
service account, which needs read access to `activiti-snapshots`. It exports
`CLOUDSMITH_USERNAME` and a short-lived `CLOUDSMITH_API_KEY` for subsequent Maven
steps; no static Cloudsmith credential is required in GitHub secrets.

The `cloudsmith-activiti-snapshots` mirror in `settings.xml` redirects the
`activiti-snapshots` repository to
`https://dl.cloudsmith.io/basic/${CLOUDSMITH_NAMESPACE}/activiti-snapshots/maven/`.
Its server entry uses the exported credentials. Every CI job that runs Maven
authenticates separately, including reusable build/test jobs, CodeQL, and
dependency submission.

The common Maven build also checks snapshot consumption with an empty local
repository. It downloads `org.activiti:activiti-engine` at the configured Activiti
version and verifies that Maven records `cloudsmith-activiti-snapshots` as the JAR's
origin. This provides evidence of a Cloudsmith download even when the same preview
exists in Nexus. Successful verification is written to the job summary.

Consuming an Activiti preview does not require the `preview` label on this
repository's PR. That label publishes an activiti-cloud preview for downstream
consumers.

## Preview Propagation

This repository includes an automated **Preview Propagation** mechanism for testing cross-module integration with downstream repositories like hxp-common-libraries. When working on features that span multiple modules:

1. Add labels `preview` + `preview-propagation` to your PR
2. The system automatically builds a preview version and creates a test PR in hxp-common-libraries
3. Test the integration in the downstream PR
4. When your source PR closes/merges, the test PR automatically closes

This eliminates manual version bumping and cleanup while ensuring integration changes are validated before merge.

## Formatting

The local `.editorconfig` file is leveraged for automated formatting.

See documentation at [pre-commit](https://github.com/Alfresco/alfresco-build-tools/tree/master/docs#pre-commit).

To run all hooks locally:

```sh
pre-commit run -a
```
