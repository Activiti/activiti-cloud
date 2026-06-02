# Serenity retirement report

Last updated: 2026-05-27

## Playwright today

| Metric                       | Value                                                              |
| ---------------------------- | ------------------------------------------------------------------ |
| Spec files                   | 8                                                                  |
| Tests (`npm run test:all`)   | **58**                                                             |
| Smoke (`npm run test:smoke`) | **18**                                                             |
| CI                           | Playwright on full messaging matrix (`.github/workflows/main.yml`) |
| Workers                      | 4 (CI) / 2 (local default); override with `PLAYWRIGHT_WORKERS`     |

## Serenity vs Playwright coverage

| Area                      | Serenity (approx.) | Playwright | Serenity module status              |
| ------------------------- | ------------------ | ---------- | ----------------------------------- |
| Identity adapter          | —                  | 9 tests    | N/A (Playwright-only)               |
| Security hruser / hradmin | 10 scen.           | 9 tests    | Modules removed from scenarios      |
| Multi-runtime signal      | 1 scen.            | 1 test     | Module removed                      |
| process-instance-actions  | 23 scen.           | 21+ tests  | **Removed** from scenarios          |
| task-actions wave 1       | 10 scen.           | 10 tests   | **Removed**                         |
| task-actions wave 2       | 19 scen.           | partial    | **Partial**                         |
| Runtime bundle (rest)     | ~54 scen.          | partial    | **Blocked** — remainder in `.story` |

## What remains in the repo (Serenity)

| Asset                                  | Reason                                    |
| -------------------------------------- | ----------------------------------------- |
| `activiti-cloud-acceptance-scenarios/` | Legacy `runtime-acceptance-tests` stories |
| `activiti-cloud-acceptance-tests/`     | Shared Serenity libraries + Maven reactor |

Do **not** delete until remaining runtime stories are migrated or waived and CI no longer depends on Serenity.

## Recommended next steps

1. Finish `task-actions` wave 2 + service-tasks / connectors / timers stories.
2. Delete `activiti-cloud-acceptance-scenarios/` when runtime gate is Playwright-only.
3. Trim Maven reactor / root `pom.xml` Serenity modules.

## Environment isolation (developers)

| Risk               | Mitigation                                                                            |
| ------------------ | ------------------------------------------------------------------------------------- |
| Same namespace     | Unique `ACCEPTANCE_ENV_NAME` (`npm run test:setup -- --new-env`)                      |
| Port-forward clash | Playwright global-setup owns `8080`; manual `npm run port-forward` for debugging only |
| Prereqs race       | One `cluster:prereqs` at a time per namespace                                         |
| Leftover data      | `dirtyRegistry` cleanup; use own preview on shared clusters                           |

See [PARALLEL_SAFE.md](PARALLEL_SAFE.md).
