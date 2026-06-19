# CI alignment with process-services / studio-services

Aligned with [hxp-process-services](https://github.com/Alfresco/hxp-process-services) build layout while keeping activiti-cloud specifics (BOM gate, Playwright matrix, library-only modules).

## Build layout

| Area             | Implementation                                                                                                                                                                                                                                   |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Build matrix     | Dynamic from [`docker-scan-image-dirs`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/docker-scan-image-dirs/action.yml) + [`.github/ci/docker-image-services.json`](../../.github/ci/docker-image-services.json) |
| Maven invocation | Root reactor: `-pl <extra-modules><image-dir> -am -Dskip.common.tests=true`                                                                                                                                                                      |
| Library modules  | [`maven-build-libraries`](../../.github/workflows/_reusable-maven-library-build.yml) for modules without Docker images                                                                                                                           |
| Common deps      | `maven-build-common` once; per-service jobs rely on **local `-am` rebuild** (no `m2-common` download)                                                                                                                                            |
| Common tests     | Once in `maven-build-common` (`activiti-cloud-api`, `activiti-cloud-service-common`); skipped in service builds via `skip.common.tests`                                                                                                          |
| M2 artifacts     | Per-service / per-library upload via `maven-build` (`m2-current-build-upload-name`); `maven-m2-upload` kept for `maven-build-common`                                                                                                             |
| Docker           | `make docker/<short-name>` on scanned image dir                                                                                                                                                                                                  |
| Build action     | [`maven-build`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/maven-build/action.yml) per matrix cell (Java 25 / Corretto)                                                                                        |

## Docker image matrix (4 services)

Discovered by `docker-scan-image-dirs` under:

- `./activiti-cloud-examples/example-runtime-bundle`
- `./activiti-cloud-examples/example-cloud-connector`
- `./activiti-cloud-examples/activiti-cloud-query`
- `./activiti-cloud-examples/activiti-cloud-identity-adapter`

Extra `-pl` modules are in [`.github/ci/docker-image-services.json`](../../.github/ci/docker-image-services.json). Merge script: [`scripts/ci/transform-docker-image-matrix.sh`](../../scripts/ci/transform-docker-image-matrix.sh).

## Library-only modules (no Docker image)

Built in parallel via [`.github/ci/library-modules.json`](../../.github/ci/library-modules.json):

- `messages-graphql` — messages + notifications-graphql services
- `audit` — audit service (no example image)

Tests still run in `maven-test` shards (`messages-graphql`, `query-audit`).

## `skip.common.tests`

Property on **`activiti-cloud-api`** and **`activiti-cloud-service-common`** parent POMs only.

- `maven-build-common`: install **without** `-Dskip.common.tests=true` for api + service-common → common tests run once
- Per-service / library: `mvn install -Dskip.common.tests=true -pl … -am` → common recompiled, tests skipped

## Test sharding

[`maven-shards.json`](../../.github/ci/maven-shards.json) is **test-only** (5 shards). The `core` shard covers `activiti-cloud-build` aggregator tests; api/service-common tests run in `maven-build-common`.

## Follow-up

- Validate whether `maven-build-dependencies-bom` can be folded into service builds once BOM ordering is proven
- Optionally adopt `maven-build` for `maven-build-common` (multi-root `-f` install is still custom)

## process-services reference commands

```yaml
# build-common
maven-command: install -T 1C … -pl <all-common-modules> -am

# per-service matrix cell
MODULE_OPTIONS: -pl ${{ matrix.extra-modules }}${{ matrix.path }} -am
maven-command: install -T 1C … -Dskip.common.tests=true $MODULE_OPTIONS
```
