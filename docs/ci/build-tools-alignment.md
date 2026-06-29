# CI alignment with alfresco-build-tools

Aligned with [`alfresco-build-tools`](https://github.com/Alfresco/alfresco-build-tools) patterns (docker matrix, `maven-build`, `maven-tag`, `create-tag`).

## Build layout

| Area           | Implementation                                                                                                                                                                                                                      |
| -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Parallel graph | `build-common`, per-image build/test, and BOM start together after `create-tag`                                                                                                                                                     |
| Versioning     | [`maven-tag`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/maven-tag/action.yml) on push; PR preview via `maven-build` / `preview` label                                                            |
| Build matrix   | [`docker-scan-image-dirs`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/docker-scan-image-dirs/action.yml) + [`.github/ci/docker-image-services.json`](../../.github/ci/docker-image-services.json) |
| Test matrix    | Same docker matrix (audit + messages-graphql built with query cell)                                                                                                                                                                 |
| Maven          | Root `mvn … -pl <modules> -am` via [`maven-build`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/maven-build/action.yml)                                                                             |
| Maven goals    | Per-image **build**: `install -DskipTests` (classes + Docker). **Test**: `verify -Dmaven.install.skip=true` (no second install). **Common**: `install` with tests, then JaCoCo only on build parent                                 |
| Common tests   | Once in `maven-build-common`; skipped in service cells via `skip.common.tests`                                                                                                                                                      |
| JaCoCo / Sonar | Per-cell `jacoco-report-name` + `target-*` / `target-test-*` uploads; `compile` then `sonar:sonar`                                                                                                                                  |
| Docker         | `make docker/<short-name>` after image-dir build                                                                                                                                                                                    |
| Playwright     | Starts when per-image **build** (with Docker) completes                                                                                                                                                                             |

## Docker image matrix (4 services)

- `./activiti-cloud-examples/example-runtime-bundle`
- `./activiti-cloud-examples/example-cloud-connector`
- `./activiti-cloud-examples/activiti-cloud-query` (`extraModules` includes audit + messages-graphql; `testMavenFlags`: `-T 1 -DunitTests.parallel=false`)
- `./activiti-cloud-examples/activiti-cloud-identity-adapter`

Merge script: [`scripts/ci/transform-docker-image-matrix.sh`](../../scripts/ci/transform-docker-image-matrix.sh).

## `skip.common.tests`

On `activiti-cloud-api` and `activiti-cloud-service-common` parent POMs. Service cells pass `-Dskip.common.tests=true`.

## Removed (POC cleanup)

- `maven-shards.json`, `maven-jacoco-merge`, custom `maven-setup` / `maven-m2-*` / `resolve-version` actions
- `library-modules.json`, library-only reusable workflows
- `validate-ci-matrix-lists.sh` (matrix is generated from Dockerfiles)
