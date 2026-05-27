# Parallel-safe test rules

## Isolation model

| Layer              | Mechanism                                                                            |
| ------------------ | ------------------------------------------------------------------------------------ |
| **Kubernetes**     | One preview namespace per env (`pr-{ACCEPTANCE_ENV_NAME}-rabbit-n-d`)                |
| **Worker**         | `testScope.prefix` = `pw-w{worker}p{slot}-{time}-` on names / business keys          |
| **Test**           | `dirtyRegistry` DELETE after each test (Serenity `@AfterScenario` parity)            |
| **Cluster config** | `cluster:prereqs` is idempotent but not concurrent — one run per namespace at a time |

## Do

- Start processes via `runtimeBundleService*.startProcess()` — auto unique `name` / `businessKey` + cleanup.
- Create standalone tasks via `taskService*.createStandaloneTask()` — tracked + cleaned up.
- Use `testScope` + `scopedName()` when a test asserts on a specific process name (LIKE queries, rename).
- Use `expect.poll()` for async engine/query sync.
- Give each developer their own `ACCEPTANCE_ENV_NAME` on shared clusters.

## Do not

- Hardcode `businessKey: 'businessKey'` or shared `processInstanceName` across tests.
- Rely on “empty” query lists without scoping by id or unique name prefix.
- Run two `cluster:prereqs` / `global-setup` patches on the **same** namespace simultaneously.
- Run two Playwright suites on the same machine without coordination (each global-setup tries port `8080`); use different `LOCAL_PORT` or run one suite at a time.
- Use `workers > 1` for tests marked `@serial` (when introduced).
- Filter slow integration: `npm run test -- --grep @slow` (e.g. multi-RB signal spec).

## Fixtures

- `testScope` — from `getTestScope(testInfo)`
- `dirtyRegistry` — auto `cleanup()` after each test via Playwright `test.step` + `Logger` (same format as API calls); set `ACCEPTANCE_CLEANUP_VERBOSE=true` for colored persona logs
- Global setup / teardown — `config/lifecycle/` + `helpers/acceptance-progress.ts`; dotenv via `config/load-env.ts`

## CI / local

```bash
# default 4 workers (override)
PLAYWRIGHT_WORKERS=6 npm run test:runtime
```
