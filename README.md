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

### GitHub Actions lockfile

GitHub Actions dependencies are tracked in [`.github/workflows/actions.lock`](.github/workflows/actions.lock) using [gh-actions-lock](https://github.com/github/gh-actions-lock). The lockfile records immutable commit SHAs for direct and transitive action dependencies, making dependency changes visible during review even when workflows reference version tags.

#### Setup

Install the [GitHub CLI](https://cli.github.com/), authenticate with GitHub, and install the extension:

```bash
gh auth login
gh extension install github/gh-actions-lock
```

#### Updating workflows and actions

After adding, removing, or updating an action reference, run these commands from the repository root:

```bash
gh actions-lock
gh actions-lock --verify
```

The first command updates the lockfile and may update action references in workflows. The second verifies dependencies without changing files. Review and commit the generated lockfile together with any workflow or action changes; do not edit the lockfile manually.

For repository-local actions, use repository-root references such as:

```yaml
- uses: $/.github/actions/setup-java-maven
```

The extension supports migrating existing `./` references with `gh actions-lock --migrate-local-actions`.

For a quick offline coverage check, run `gh actions-lock --verify-local`. This checks that action references have lockfile entries, but does not validate their trustworthiness or detect moved upstream tags. Use `gh actions-lock --verify` for full verification.

If verification reports a moved upstream reference, investigate the change before accepting a new SHA. After confirming the change is expected, run `gh actions-lock --accept-moved`, review the diff, and verify again. Do not refresh pins blindly to silence a warning.

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
