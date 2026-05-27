# Activiti Cloud — Playwright acceptance tests

API acceptance tests for Activiti Cloud preview installs. **Playwright-only** target; Serenity is being retired ([docs/SERENITY_RETIREMENT.md](docs/SERENITY_RETIREMENT.md)).

## Requirements

| Tool        | Version                        |
| ----------- | ------------------------------ |
| Node.js     | **22 LTS** (CI uses 22)        |
| npm         | with Node                      |
| kubectl     | access to your preview cluster |
| Helm / `yq` | first-time preview only        |

Browsers: `npm run install:browsers` (Chromium is enough for API tests).

## Quick start

Run everything from the **repository root**.

### 1. Install dependencies

```bash
npm install
npm run install:browsers
```

### 2. Point kubectl at Activiti (important)

`kubectl` uses **`KUBECONFIG`**, not `ACTIVITI_KUBECONFIG`. Either:

```bash
export ACTIVITI_KUBECONFIG=~/Downloads/activiti.yaml
npm run kube:use
kubectl config current-context    # should show activiti, not another cluster
```

or:

```bash
export KUBECONFIG=~/Downloads/activiti.yaml
```

`npm run test:setup` runs `kube:use` for you; **manual** `kubectl` commands need one of the options above.

### 3. First-time preview + `.env`

```bash
npm run test:setup -- --install
```

This does, in order:

1. Resolve latest Docker image tags → `local-values.local.yaml` (once)
2. Helm install into a **new** namespace (`pr-<user>-<random>-rabbit-n-d` by default)
3. Write `activiti-cloud-acceptance-tests-playwright/.env`
4. Apply acceptance cluster prerequisites (hostAliases, policies, RB image) — **one** rollout pass

| Flag                | When to use                                                                |
| ------------------- | -------------------------------------------------------------------------- |
| `--install`         | Create preview if missing (required first time)                            |
| `--new-env`         | New random env name; ignore existing `.env`                                |
| `--name alice`      | Fixed env name → `pr-alice-rabbit-n-d` (keep under ~13 chars for env name) |
| `--no-install`      | Only refresh `.env` / prereqs on an existing preview                       |
| `--kubeconfig PATH` | Override kubeconfig for this run                                           |

Examples:

```bash
npm run test:setup -- --install                    # default: michal-a3f2b1 → pr-michal-a3f2b1-rabbit-n-d
npm run test:setup -- --install --name my-feature  # stable name you choose
npm run test:setup -- --install --new-env          # new namespace + new .env
```

**Shared cluster:** each developer gets their own namespace. Old names like `activiti-tests` in `.env` are ignored on the next setup.

**Namespace length:** preview name must be ≤ 29 characters (Kubernetes limit for identity-adapter). Default `$USER-<random>` fits; long `--name` values are rejected with a clear error.

### 4. Run tests

Port-forward starts automatically in Playwright global-setup (no second terminal).

```bash
npm run check:env
npm run test:smoke          # @smoke
npm run test:all            # full suite
```

`npm run port-forward` is only for manual `curl` debugging.

### 5. Cleanup

```bash
npm run preview:delete
# uses PREVIEW_NAME from .env
```

Or:

```bash
PREVIEW_NAME=pr-my-env-rabbit-n-d make delete
```

List your previews:

```bash
npm run kube:use
kubectl get ns | grep '^pr-'
```

## Command reference

| Command                           | Purpose                                                |
| --------------------------------- | ------------------------------------------------------ |
| `npm run kube:use`                | Set `KUBECONFIG` from `ACTIVITI_KUBECONFIG`            |
| `npm run test:setup -- --install` | Helm preview + `.env` + cluster prereqs                |
| `npm run test:setup`              | Refresh `.env` / prereqs only (existing preview)       |
| `npm run cluster:prereqs`         | Host aliases, security policies, RB image (idempotent) |
| `npm run preview:delete`          | `helm uninstall` + delete namespace from `.env`        |
| `npm run check:env`               | Validate `.env` and connectivity                       |
| `npm run verify:process-catalog`  | BPMN keys on runtime-bundle                            |
| `npm run test:smoke` / `test:all` | Playwright suites                                      |
| `npm run report`                  | Open last HTML report                                  |

### Run tests by area

```bash
npm run test:identity
npm run test:security
npm run test:runtime
npm run test:runtime:tasks
npm run test:all

npm run test:debug
PLAYWRIGHT_WORKERS=6 npm run test:runtime
```

### CI matrix locally (optional)

Same combinations as GitHub Actions — install + test in one npm script:

```bash
npm run matrix:rabbitmq:non-partitioned:default --name=my-matrix-dev
npm run matrix:kafka:partitioned:override --name=kafka-ovr
```

Or install manually, then Playwright:

```bash
export KUBECONFIG=~/Downloads/activiti.yaml
./scripts/local-install.sh -n local-dev -b rabbitmq -p non-partitioned --destinations-option default-destinations
npm run test:setup          # .env + prereqs only
npm run test:all
```

To refresh Docker tags without full setup: `REFRESH_LOCAL_IMAGE_TAGS=true ./scripts/local-install.sh -n ...`

## Configuration (`.env`)

Generated by `npm run test:setup` (overwritten each run). See [`.env.example`](.env.example) for all variables.

| Variable                          | Purpose                                          |
| --------------------------------- | ------------------------------------------------ |
| `PREVIEW_NAME`                    | K8s namespace (e.g. `pr-jane-a3f2b1-rabbit-n-d`) |
| `ACCEPTANCE_ENV_NAME`             | Short env id passed to Helm (`jane-a3f2b1`)      |
| `CLUSTER_NAME` / `CLUSTER_DOMAIN` | DNS segment for gateway / identity               |
| `GATEWAY_HOST`                    | Gateway via port-forward                         |
| `SSO_HOST`                        | Keycloak token URL (realm `activiti`)            |
| `KEYCLOAK_CLIENT_SECRET`          | From secret `activiti-keycloak-client`           |
| `TESTUSER_*`, `HRUSER_*`, …       | Seeded users (password `password`)               |

| Optional               | Default | Purpose                                   |
| ---------------------- | ------- | ----------------------------------------- |
| `AUTO_CLUSTER_PREREQS` | `true`  | Patch cluster in global-setup when needed |
| `PLAYWRIGHT_WORKERS`   | `4`     | Parallel workers                          |
| `ACTIVITI_KUBECONFIG`  | —       | Path for `npm run kube:use`               |

## Cluster prerequisites

Some previews need hostAliases, security policies, and the `example-runtime-bundle` image. Applied by `test:setup` and again in global-setup when `AUTO_CLUSTER_PREREQS=true` (skips work already done).

```bash
npm run cluster:prereqs
```

Do not run prereqs twice in parallel on the same namespace.

## CI/CD

- Workflow: `.github/workflows/main.yml` → Playwright via `.github/actions/playwright-run`
- Playwright `test:all` runs on each messaging matrix cell ([retirement plan](docs/SERENITY_RETIREMENT.md))
- Retries: `2` on CI; artifacts: JUnit, JSON, HTML; trace on first retry

### CI matrix (messaging)

| Job                | Broker   | Partitioning    | Destinations |
| ------------------ | -------- | --------------- | ------------ |
| baseline           | rabbitmq | non-partitioned | default      |
| rabbit partitioned | rabbitmq | partitioned     | default      |
| kafka              | kafka    | non-partitioned | default      |
| kafka partitioned  | kafka    | partitioned     | default      |
| kafka override     | kafka    | partitioned     | override     |
| rabbit pdb         | rabbitmq | non-partitioned | pdb          |
| rabbit prefix pdb  | rabbitmq | prefix          | pdb          |

## Project structure

```text
activiti-cloud-acceptance-tests-playwright/
├── tests/                 # Specs (*.spec.ts)
├── fixtures/              # Playwright fixtures
├── services/              # API clients
├── config/                # connection, lifecycle (see config/README.md)
├── scripts/               # setup, prereqs, port-forward, matrix
└── docs/                  # PARALLEL_SAFE, SERENITY_RETIREMENT
```

## Writing tests (parallel-safe)

1. Use `activiti` from `fixtures/services.fixture.ts`.
2. Start processes with `runtimeBundleService.startProcess()` — unique names + auto cleanup.
3. For fixed names: `scopedName(testScope, 'my-label')`.
4. Use `expect.poll()` — no `sleep()`.
5. Do not depend on execution order.

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
- [docs/SERENITY_RETIREMENT.md](docs/SERENITY_RETIREMENT.md) — Serenity vs Playwright status
- [docs/PARALLEL_SAFE.md](docs/PARALLEL_SAFE.md) — isolation rules
