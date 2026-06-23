# CI alignment with process-services / studio-services

Aligned with [hxp-process-services](https://github.com/Alfresco/hxp-process-services) and Elias review on PR #2314.

## Build layout

| Area | Implementation |
| ---- | -------------- |
| Parallel graph | `build-common`, per-image build/test, library build/test, and BOM start together after `resolve-version` |
| Build matrix | [`docker-scan-image-dirs`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/docker-scan-image-dirs/action.yml) + [`.github/ci/docker-image-services.json`](../../.github/ci/docker-image-services.json) |
| Test matrix | Same docker matrix + [`.github/ci/library-modules.json`](../../.github/ci/library-modules.json) |
| Maven | Root `mvn … -pl <modules> -am` via [`maven-build`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/maven-build/action.yml) |
| Common tests | Once in `maven-build-common`; skipped in service cells via `skip.common.tests` |
| JaCoCo / Sonar | Per-cell `jacoco-report-name` + `target-*` uploads; [`sonar-scan-on-built-project`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/sonar-scan-on-built-project/action.yml) |
| Docker | `make docker/<short-name>` after image-dir build |
| Playwright | Starts when per-image **build** (with Docker) completes |

## Docker image matrix (4 services)

- `./activiti-cloud-examples/example-runtime-bundle`
- `./activiti-cloud-examples/example-cloud-connector`
- `./activiti-cloud-examples/activiti-cloud-query` (`testMavenFlags`: `-T 1 -DunitTests.parallel=false`)
- `./activiti-cloud-examples/activiti-cloud-identity-adapter`

Merge script: [`scripts/ci/transform-docker-image-matrix.sh`](../../scripts/ci/transform-docker-image-matrix.sh).

## Library-only modules

- `messages-graphql`, `audit` — separate build/test reusable workflows

## `skip.common.tests`

On `activiti-cloud-api` and `activiti-cloud-service-common` parent POMs. Service/library cells pass `-Dskip.common.tests=true`.

## Removed (POC cleanup)

- `maven-shards.json`, `maven-jacoco-merge`, custom `maven-setup` / `maven-m2-*` actions
- `validate-ci-matrix-lists.sh` (matrix is generated from Dockerfiles)
