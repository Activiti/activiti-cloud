# Activiti Cloud — Playwright acceptance tests

API acceptance tests for Activiti Cloud preview installs. **Playwright-only** target; Serenity is being retired ([docs/SERENITY_RETIREMENT.md](docs/SERENITY_RETIREMENT.md)).

## Requirements

| Tool                      | Version                        |
| ------------------------- | ------------------------------ |
| Node.js                   | **22 LTS** (CI uses 22)        |
| npm                       | with Node                      |
| kubectl                   | access to your preview cluster |
| Helm / `local-install.sh` | first-time preview only        |

Browsers: `npm run install:browsers` (Chromium is enough for API tests).

## Quick start

```bash
# repo root
npm install
npm run install:browsers

export ACTIVITI_KUBECONFIG=~/Downloads/activiti.yaml   # your kubeconfig

# First time: install preview + generate .env (Helm chart includes Keycloak in the namespace)
npm run test:setup -- --install --name activiti-tests

# Verify + run (port-forward starts automatically in global-setup — no second terminal)
npm run check:env
npm run test:smoke          # @smoke tag
npm run test:all            # full suite
```

`npm run port-forward` is only for manual debugging without Playwright (e.g. `curl` against `localhost:8080`).

**Shared cluster?** Use a **unique** env name per developer: `--name alice` → namespace `pr-alice-rabbit-n-d`. See [docs/PARALLEL_SAFE.md](docs/PARALLEL_SAFE.md).

## Configuration (`.env`)

`.env` is **generated automatically** by `npm run test:setup` and is **always overwritten** (single source of truth).

Use [`.env.example`](.env.example) only as a reference for variables.

| Variable                          | Purpose                                             |
| --------------------------------- | --------------------------------------------------- |
| `PREVIEW_NAME`                    | K8s namespace (e.g. `pr-activiti-tests-rabbit-n-d`) |
| `CLUSTER_NAME` / `CLUSTER_DOMAIN` | Gateway hostnames                                   |
| `SSO_HOST`                        | Keycloak token URL (preview realm `activiti`)       |
| `KEYCLOAK_CLIENT_ID`              | `activiti`                                          |
| `KEYCLOAK_CLIENT_SECRET`          | From secret `activiti-keycloak-client`              |
| `GATEWAY_HOST`                    | `gateway-{preview}.{cluster}.{domain}:8080`         |
| `TESTUSER_*`, `HRUSER_*`, …       | Seeded users (password `password`)                  |

Optional:

| Variable               | Default | Purpose                                   |
| ---------------------- | ------- | ----------------------------------------- |
| `AUTO_CLUSTER_PREREQS` | `true`  | Patch cluster in global-setup when needed |
| `PLAYWRIGHT_WORKERS`   | `4`     | Parallel workers                          |
| `LOCAL_PORT`           | `8080`  | Port-forward local port                   |
| `ACTIVITI_KUBECONFIG`  | —       | Kubeconfig path                           |

## Run tests

```bash
npm run test:smoke              # fast subset
npm run test:identity
npm run test:security
npm run test:runtime            # all tests/runtime/
npm run test:runtime:tasks
npm run test:all

npm run test:debug              # Playwright inspector
npx playwright test tests/runtime/task.spec.ts --headed   # single file

PLAYWRIGHT_WORKERS=6 npm run test:runtime   # more parallelism
npm run report                  # HTML report
```

## Cluster prerequisites

Some previews need hostAliases, security policies, and `example-runtime-bundle` image:

```bash
npm run cluster:prereqs
```

Applied automatically in global-setup when `AUTO_CLUSTER_PREREQS=true` (idempotent). Live, colored progress logs per service persona.

**Do not** run prereqs twice in parallel on the same namespace.

## CI/CD

- Workflow: `.github/workflows/main.yml` → Playwright via `.github/actions/playwright-run`
- Serenity `make test/runtime-acceptance-tests` removed from CI; Playwright `test:all` is the gate ([retirement plan](docs/SERENITY_RETIREMENT.md))
- Retries: `2` on CI
- Artifacts: JUnit, JSON, HTML report; trace on first retry; screenshot/video on failure

## Project structure

```
activiti-cloud-acceptance-tests-playwright/
├── tests/                 # Specs (*.spec.ts)
│   ├── runtime/           # Migrated Serenity runtime stories
│   └── *security*.spec.ts
├── fixtures/              # Playwright fixtures (users, services, cleanup)
├── services/              # API clients (runtime, query, audit, …)
├── helpers/               # dirty-context, test-isolation, deployment
├── config/                # connection, runtime, validation, lifecycle (see config/README.md)
├── models/                # TypeScript API models
├── scripts/               # setup, prereqs, port-forward
└── docs/                  # PARALLEL_SAFE, SERENITY_RETIREMENT
```

## Writing tests (parallel-safe)

1. Use `activiti` from `fixtures/services.fixture.ts` (includes `testScope` + `dirtyRegistry`).
2. Start processes with `runtimeBundleService.startProcess()` — unique name/businessKey + auto cleanup.
3. For fixed names (LIKE queries): `scopedName(testScope, 'my-label')`.
4. Wait with `expect.poll()` — no `sleep()` / hardcoded delays.
5. Do not depend on execution order or shared global state.

Details: [docs/PARALLEL_SAFE.md](docs/PARALLEL_SAFE.md).

## Diagnostics

| Feature                          | When                               |
| -------------------------------- | ---------------------------------- |
| Trace                            | CI: first retry; local: on failure |
| Screenshot / video               | On failure                         |
| `npm run check:env`              | Before first run                   |
| `npm run verify:process-catalog` | BPMN keys on runtime-bundle        |

## Related docs

- [MIGRATION_PLAN.md](MIGRATION_PLAN.md) — story-by-story migration tracker
- [docs/SERENITY_RETIREMENT.md](docs/SERENITY_RETIREMENT.md) — what can be deleted vs blocked
- [docs/PARALLEL_SAFE.md](docs/PARALLEL_SAFE.md) — isolation rules
