# Serenity retirement report

Last updated: 2026-05-27

## Playwright infrastructure (this change)

| Item | Status |
|------|--------|
| Dirty-context cleanup (`DirtyContextRegistry`) | **Done** — after each test |
| Worker-scoped names / business keys | **Done** — `testScope` fixture |
| Default `PLAYWRIGHT_WORKERS=4` | **Done** |
| Trace on retry (CI) / retain on failure (local) | **Done** |
| `docs/PARALLEL_SAFE.md` | **Done** |

## Serenity vs Playwright coverage

| Area | Serenity | Playwright | Remove Serenity? |
|------|----------|------------|------------------|
| Identity adapter | — | 9 tests | N/A |
| Security hruser | 6 scen. | 7 tests | **Ready** (PW is source of truth) |
| Security hradmin | 4 scen. | 2 tests | **Ready** (consolidated) |
| Multi-runtime signal | 1 scen. | 1 test | **Ready** (verify 2 RB when deployed) |
| process-instance-actions | 23 scen. | 21 tests | **Ready** |
| task-actions wave 1 | 10 scen. | 10 tests | **Ready** |
| task-actions wave 2 | 19 scen. | 8 tests (partial) | **In progress** |
| Runtime bundle (rest) | ~54 scen. | partial | **Blocked** — remainder not migrated |
| multiple-runtime module | few | partial | **Blocked** |

## What was NOT removed (and why)

| Asset | Reason |
|-------|--------|
| `activiti-cloud-acceptance-scenarios/` | Serenity removed from CI gate; module kept until full migration |
| `activiti-cloud-acceptance-tests/` (Serenity libs) | Shared by scenarios + Maven reactor |
| `serenity-*` Maven deps | Required until CI gate switches to Playwright-only |

**Do not delete Serenity modules until:**

1. Remaining ~73 runtime scenarios are migrated or explicitly waived.
2. `main.yml` `acceptance-tests` job runs Playwright as **required** and Serenity is removed or `continue-on-error`.
3. `grep serenity` in CI/docs is intentional zero.

## Recommended next steps (priority order)

1. ~~Stabilize PW with `dirtyRegistry` + 4 workers on CI~~ — done (`PLAYWRIGHT_WORKERS=4`).
2. Finish `task-actions` wave 2 (remaining ~11 scenarios) + service-tasks stories.
3. ~~Switch CI gate: Playwright required, Serenity off~~ — done in `main.yml`.
4. Delete `activiti-cloud-acceptance-scenarios/` and trim `activiti-cloud-acceptance-tests/pom.xml`.
5. Remove Serenity from Maven reactor when no longer referenced.

## Environment isolation (developers)

| Risk | Mitigation |
|------|------------|
| Same namespace | Unique `ACCEPTANCE_ENV_NAME` per person (`npm run test:setup -- --name alice`) |
| Same port-forward | Playwright starts port-forward in global-setup; manual `npm run port-forward` only for debugging |
| Prereqs race | Don't run `cluster:prereqs` twice in parallel on same namespace |
| Leftover data | Mitigated by `dirtyRegistry`; not a substitute for unique preview per team |
