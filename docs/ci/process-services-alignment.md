# CI alignment with process-services / studio-services (planned)

Target: match [hxp-process-services](https://github.com/Alfresco/hxp-process-services) build layout while keeping activiti-cloud specifics (BOM gate, Playwright matrix, library-only modules).

## Current POC vs target

| Area | Current POC | Target (process-services) |
| ---- | ----------- | ------------------------- |
| Build matrix | 5 Maven shards (`maven-shards.json`) | Dynamic matrix from [`docker-scan-image-dirs`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/docker-scan-image-dirs/action.yml) |
| Maven invocation | `-pl <shard-modules> -am` per shard | Root reactor: `-pl <extra-modules><image-dir> -am -Dskip.common.tests=true` |
| Common deps | `maven-build-common` + **download `m2-common`** in every shard | `build-common` once; per-service jobs rely on **local `-am` rebuild** (no m2-common download) |
| Common tests | Once in `maven-test (core)` | Once in `build-common`; skipped in service builds via `skip.common.tests` |
| M2 artifacts | Per-shard upload/download | Per-service upload; download **only for Sonar** aggregation |
| Docker | `make docker/<name>` from shard config | `docker-build-and-push-image` (or keep Make) on scanned image dir |
| Build action | Custom `maven-setup` + shell `mvn` | [`maven-build`](https://github.com/Alfresco/alfresco-build-tools/blob/master/.github/actions/maven-build/action.yml) per matrix cell (follow-up) |

## Docker image matrix (4 services)

Discovered by `docker-scan-image-dirs` under:

- `./activiti-cloud-examples/example-runtime-bundle`
- `./activiti-cloud-examples/example-cloud-connector`
- `./activiti-cloud-examples/activiti-cloud-query`
- `./activiti-cloud-examples/activiti-cloud-identity-adapter`

Extra `-pl` modules (not always pulled by `-am` alone) are in [`.github/ci/docker-image-services.json`](../../.github/ci/docker-image-services.json). Merge script: [`scripts/ci/transform-docker-image-matrix.sh`](../../scripts/ci/transform-docker-image-matrix.sh).

## `skip.common.tests` (landed)

Property on **`activiti-cloud-api`** and **`activiti-cloud-service-common`** parent POMs only (same idea as `alfresco-process-common-parent` in process-services). Service modules inherit `activiti-cloud-build-parent` directly, so their tests still run in per-service builds.

- `build-common`: `mvn install …` **without** `-Dskip.common.tests=true` → common tests run once
- Per-service: `mvn install -Dskip.common.tests=true -pl … -am` → common recompiled, tests skipped

## Library-only modules (no Docker image)

Still need CI coverage after image-matrix migration:

- `activiti-cloud-messages-service`, `activiti-cloud-notifications-graphql-service`
- `activiti-cloud-audit-service` (no image in examples)
- `activiti-cloud-build` / aggregator tests

Options: dedicated `library-build` job(s) or extend `build-common` test scope.

## Suggested migration steps

1. **Done (prep):** `skip.common.tests`, `docker-image-services.json`, `transform-docker-image-matrix.sh`
2. Add `scan-image-dirs` job; switch `maven-build` matrix to `fromJSON(transformed-json)`
3. Replace shard `mvn -pl` with `maven-build` + `-Dskip.common.tests=true` + `MODULE_OPTIONS`
4. Remove `maven-m2-download` / `m2-common` from per-service build jobs
5. Keep `maven-build-dependencies-bom` until BOM ordering is validated without shard M2
6. Sonar: download all `m2-*` / `target-*` artifacts only in `sonar` job
7. Deprecate `maven-shards.json` build section; retain test sharding or align tests per service

## process-services reference commands

```yaml
# build-common
maven-command: install -T 1C … -pl <all-common-modules> -am

# per-service matrix cell
MODULE_OPTIONS: -pl ${{ matrix.extra-modules }}${{ matrix.path }} -am
maven-command: install -T 1C … -Dskip.common.tests=true $MODULE_OPTIONS
```
