---
applyTo: "activiti-cloud-acceptance-tests-playwright/**/*.ts"
---

# Activiti Cloud Acceptance Tests (Playwright API) — Authoring Guidelines

These rules apply whenever you generate, modify, or review Playwright acceptance test code in **activiti-cloud** (`activiti-cloud-acceptance-tests-playwright/`).

These are **API-level** acceptance tests — not browser UI tests. There are no page objects, locators, or `page` interactions. Tests call backend REST APIs (and GraphQL WebSocket subscriptions where needed) through typed service clients and assert on responses and payloads.

---

## Architecture Overview

```
activiti-cloud-acceptance-tests-playwright/
├── playwright.config.ts       # Projects: acceptance, notifications, destructive-last, …
├── tests/                     # Spec files (*.spec.ts) only
├── fixtures/
│   ├── context.fixture.ts     # Authenticated API contexts, exports contexts + expect
│   └── services.fixture.ts    # exports activiti (services + dirtyRegistry cleanup)
├── services/                  # REST API clients (extend BaseService)
│   └── notifications/         # GraphQL WebSocket subscriptions (engine events)
├── flows/                     # Shared multi-step process start patterns
├── helpers/                   # Cross-cutting utilities (query-sync, logging, scenario skip)
├── models/                    # DTOs, ProcessDefinitionRegistry
├── config/                    # Env, cluster, timeouts, global setup/teardown
└── resources/
    └── modeling-projects/     # BPMN + extensions deployed to preview cluster
```

See `activiti-cloud-acceptance-tests-playwright/docs/PROJECT_STRUCTURE.md` for the full layout.

### API client layer

```
ContextFactory / auth-cache
    → CustomAPIRequest (authenticated Playwright APIRequestContext)
        → BaseService (HTTP helpers)
            → Service class (e.g. RuntimeBundleService, QueryService)
```

- **Service classes** hold the service base URL and expose typed API methods.
- **Spec files** call service methods via the `activiti` fixture and assert with `expect`.
- **GraphQL WebSocket** subscriptions live under `services/notifications/` — use `openEngineEventsSubscription()` from specs; do not reimplement the WS protocol in tests.

### Fixture chain

Never skip layers or recreate services that the fixture already provides.

```
context.fixture (contexts, expect)
    → services.fixture (activiti — runtime/query/task/audit services + dirtyRegistry)
```

Import `activiti` and `expect` from `fixtures/services.fixture` — **never** from `@playwright/test` directly in spec files.

```typescript
import { activiti, expect } from "../../fixtures/services.fixture";
```

### Flows

`flows/` contains **reusable process-start orchestration** (e.g. `startCatalogProcess()`, `startCatalogProcessWithFirstTask()`). Use flows when multiple specs share the same setup sequence. Do not add assertion logic to flows.

### Helpers

`helpers/` is for **non-assertion utilities** only — polling/sync (`query-sync.ts`), logging, scenario skip registry (`acceptance-scenario.ts`). Do not add new `expect()` wrappers here; legacy `*.assertions.ts` modules exist but new assertions belong in specs.

---

## CRITICAL RULES — Hard Rejections

### 1. Never create `helpers/` directories under `tests/`

Do **not** create files like `tests/**/helpers/*.helpers.ts` that group API calls, setup flows, or assertions.

| ❌ Reject                                                     | ✅ Do instead                                               |
| ------------------------------------------------------------- | ----------------------------------------------------------- |
| `tests/runtime/helpers/notifications.helpers.ts`              | Use `services/notifications/` + inline spec code            |
| Helper that accepts a service and performs multiple API calls | Inline in spec, add to `flows/`, or extend a service method |

**Some duplication in spec files is acceptable and preferred** over opaque helper abstractions.

### 2. Never define functions inside spec files

Spec files contain `describe`, hooks, and test blocks only. No local `async function`, no nested helper functions, no factory functions.

The only exception is a **`for...of` loop at describe scope** that generates independent test blocks (data-driven tests). The loop body must contain only the test declaration.

### 3. Keep API interactions in service classes

All REST calls belong in `services/*.service.ts` (or dedicated modules like `services/notifications/`).

When a new API operation is needed:

1. Add a typed method to the appropriate service.
2. Call it from the spec via the `activiti` fixture.

Do not wrap service calls in test-level utility functions.

Do **not** add separate helper modules under `services/` for API client logic. Shared HTTP utilities belong on `BaseService` in `base.service.ts` or in `services/<service>/shared/` when service-specific.

Query and audit services follow the hxp-style layout:

- `services/query/query.service.ts`, `services/audit/audit.service.ts` — facades composing endpoint classes
- `services/query/endpoints/*.endpoint.ts`, `services/audit/endpoints/*.endpoint.ts` — one class per REST resource area
- `services/query/admin/`, `services/audit/admin/` — admin variants with the same pattern

**Specs must never contain REST paths or URLs or import service classes for status checks.** Call `build*StatusChecks()` on the fixture service instance.

### 4. Keep test data in `resources/`

Static BPMN/DMN and modeling project files belong in `resources/modeling-projects/` — not inline in spec files.

```typescript
// ❌ REJECT — BPMN built as string in spec file
const bpmn = `<?xml version="1.0" ...`;

// ✅ APPROVE — use catalog processes via ProcessDefinitionRegistry
const key = ProcessDefinitionRegistry.processDefinitionKeyMatcher("CONNECTOR_PROCESS_INSTANCE");
```

### 5. No comments or JSDoc in generated code

Code must be self-documenting through naming. Do not add JSDoc, inline comments, or AI banner comments. License headers are required by the project template — do not add explanatory comments around them.

Existing file-level docblocks that explain destructive/serial test ordering may remain when already present.

### 6. No `try/catch` in spec files

Spec files must not use `try/catch` or `try/finally` for cleanup or error handling.

| ❌ Reject                                                | ✅ Do instead                                                                                                                             |
| -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `try { ... } finally { subscription.close() }`           | `activiti.afterEach(() => activeSubscription?.close())` at describe scope                                                                 |
| `try/catch` inside `expect.poll` to swallow query errors | Inline poll logic in spec (e.g. runtime bundle first, then query) or `getQueryProcessInstanceWhenSynced` in helpers used by other modules |
| `try/catch` around assertions                            | Let Playwright fail; fix root cause or split tests                                                                                        |

Optional-error handling for polling belongs in `helpers/`, not in specs.

### 7. Cleanup belongs in hooks — never after the final assertion

- Use `beforeEach` / `afterEach` / `afterAll` for setup and teardown.
- Track resource handles at `describe` scope (e.g. WebSocket subscriptions) so hooks clean up regardless of test outcome.
- Process/task cleanup is handled by `dirtyRegistry` in the fixture — register deletes via service isolation options where applicable.
- The **last statement** in every test body must be an `expect` call (or a `test.step` whose last line is `expect`). Never put cleanup after the final assertion.

```typescript
// ❌ REJECT
activiti('...', async () => {
  const sub = await openEngineEventsSubscription(...);
  try {
    expect(...).toBe(...);
  } finally {
    sub.close();
  }
});

// ✅ APPROVE
let activeSubscription: EngineEventsSubscription | undefined;

activiti.afterEach(() => {
  activeSubscription?.close();
  activeSubscription = undefined;
});

activiti('...', async () => {
  activeSubscription = await openEngineEventsSubscription(...);
  expect(...).toBe(...);
});
```

### 8. Never import `test` or `expect` from `@playwright/test` in spec files

Always import from the project fixture chain.

```typescript
// ❌ REJECT
import { test, expect } from "@playwright/test";

// ✅ APPROVE
import { activiti, expect } from "../../fixtures/services.fixture";
```

Use `activiti` as the extended test runner (equivalent to `test` in other repos).

### 9. Never wrap `expect` in control-flow logic

Assertions must run unconditionally at the top level of the test body or inside `activiti.step()` callbacks — never guarded by branching or error handling.

| ❌ Reject                                          | ✅ Do instead                            |
| -------------------------------------------------- | ---------------------------------------- |
| `try { expect(x).toBe(1) } catch { ... }`          | Unconditional `expect`                   |
| `if (response.status === 200) { expect(body)... }` | Separate assertions with `expect.soft()` |
| Ternary around expect                              | Separate tests per branch                |

If you need to verify different outcomes, write **separate tests** or use **data-driven test generation**.

---

## Spec File Conventions

### Test naming

Use descriptive human-readable titles. Test case IDs in square brackets (e.g. `[AAE-47017]`) are optional but encouraged when linked to Jira.

```typescript
activiti('complete a process instance with subscription to PROCESS event notifications', async ({ ... }) => {
```

### Tags

Use Playwright tags to control CI project selection:

| Tag            | Effect                                                                |
| -------------- | --------------------------------------------------------------------- |
| `@destructive` | Runs in `destructive-last` project (after acceptance + notifications) |
| `@smoke`       | `smoke` project                                                       |
| `@slow`        | Avoid unless justified — prefer fixing root cause or tuning timeouts  |

Add tags via `activiti.describe` or individual tests: `{ tag: '@destructive' }`.

### Assertions

- Use `expect.soft()` for all assertions **except the last one** in a test when multiple independent checks exist in the same step.
- The final assertion in the test uses regular `expect`.
- For GraphQL notification payloads that include extra fields (e.g. `actor`), prefer `expect.objectContaining` over deep equality on partial shapes.
- **Never** place `expect` inside `try/catch`, `if/else`, `switch`, loops, or ternary expressions.

### `activiti.step()` — mandatory for multi-phase tests

Every test that performs more than one logical phase **must** wrap each phase in `activiti.step()`.

| Situation                                                   | `activiti.step()` required? |
| ----------------------------------------------------------- | --------------------------- |
| One API call + one final `expect`                           | Optional                    |
| Two or more API calls                                       | **Yes**                     |
| Subscribe → start process → wait for events → assert status | **Yes**                     |
| Polling/waiting for async result + assertion                | **Yes**                     |

Use `pollOptions()` from `config/runtime/timeouts` for `expect.poll` intervals.

### Test independence

Each test must run independently. Never rely on state created by another test in the same file.

Use unique identifiers per test:

```typescript
const businessKey = randomUUID();
```

For serial suites (`activiti.describe.configure({ mode: 'serial' })`), still avoid implicit shared state beyond intentional ordering (e.g. notifications project).

### Lifecycle hooks pattern

```typescript
activiti.describe('Suite name', () => {
  let processInstanceId: string;
  let activeSubscription: EngineEventsSubscription | undefined;

  activiti.afterEach(() => {
    activeSubscription?.close();
    activeSubscription = undefined;
  });

  activiti('should ...', async ({ runtimeBundleServiceTestAdmin }) => {
    await activiti.step('When ...', async () => { ... });
    await activiti.step('Then ...', async () => {
      expect(processInstanceId).toBeTruthy();
    });
  });
});
```

---

## Playwright Projects

CI full suite order (enforced by `dependencies` in `playwright.config.ts`):

1. **acceptance** — parallel; excludes `@destructive` and `notifications.spec.ts`
2. **notifications** — serial; `notifications.spec.ts` only
3. **destructive-last** — serial; `@destructive` tests (admin bulk-delete, etc.)

Local runs: `npm test` from `activiti-cloud-acceptance-tests-playwright/` (requires cluster preview — see README).

---

## GraphQL WebSocket (Notifications)

- Subprotocol: **`graphql-transport-ws`** (not `graphql-ws`).
- Use `openEngineEventsSubscription()` — it waits for connection ack before subscribing; do not call `awaitReady()` separately.
- Close subscriptions in `afterEach`, not in test bodies.
- Event matching: `expectedEngineEventBatch()` + `subscription.waitForExpectedEvents()`.

---

## Environment and Cluster

- Tests run against a **preview Kubernetes cluster** (port-forward / gateway URL from env).
- Setup: `npm run test:setup` deploys modeling projects and security policies.
- Do not call external public URLs for connectors — use catalog processes and in-cluster connectors from modeling projects.
- Configuration lives in `.env` / `config/connection/` — never hard-code credentials in specs.

---

## Skipping Tests

Prefer fixing over skipping. When upstream blocks a scenario:

- Use `AcceptanceScenarioMeta` + `pickScenarioTest()` from `helpers/acceptance-scenario.ts` with an `exclude` Jira ticket URL.
- Do not use bare `activiti.skip()` inline without a tracked ticket.

```typescript
const scenario: DeleteScenario = {
  title: '...',
  exclude: 'https://hyland.atlassian.net/browse/AAE-46640',
};
pickScenarioTest(activiti, scenario)(scenario.title, async () => { ... });
```

---

## Where Code Belongs — Quick Reference

| Code type                     | `tests/*.spec.ts` | `tests/**/helpers/` | `services/` | `flows/` | `helpers/`             | `resources/` |
| ----------------------------- | ----------------- | ------------------- | ----------- | -------- | ---------------------- | ------------ |
| `expect()` assertions         | ✅                | ❌                  | ❌          | ❌       | ❌ (legacy exceptions) | ❌           |
| REST / WS client logic        | ❌                | ❌                  | ✅          | ❌       | ❌                     | ❌           |
| Shared process start flows    | ❌                | ❌                  | ❌          | ✅       | ❌                     | ❌           |
| Query sync / optional polling | ❌                | ❌                  | ❌          | ❌       | ✅                     | ❌           |
| Static BPMN/projects          | ❌                | ❌                  | ❌          | ❌       | ❌                     | ✅           |
| Setup/teardown                | hooks only        | ❌                  | ❌          | ❌       | ❌                     | ❌           |
| Local helper functions        | ❌                | ❌                  | ❌          | ❌       | ❌                     | ❌           |

---

## Adding New Test Coverage — Step by Step

1. **Check existing coverage** — search `tests/` for similar specs; extend rather than duplicate.
2. **Add modeling project** under `resources/modeling-projects/` if new BPMN is needed; run setup to deploy.
3. **Add TypeScript models** under `models/` if new response types are needed.
4. **Add service methods** to the appropriate `services/*.service.ts`.
5. **Write the spec** using `activiti` fixture and `activiti.step()`.
6. **Tag** `@destructive` if the test wipes shared namespace data.
7. **Register skip** via `acceptance-scenario.ts` pattern if temporarily blocked.

---

## Code Quality

After modifying TypeScript in `activiti-cloud-acceptance-tests-playwright/`:

```bash
npm run typecheck
npm run lint:fix
```

---

## Anti-Pattern Catalogue

| #   | Anti-pattern                                                                                          | Correct approach                                                          |
| --- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| 1   | `tests/**/helpers/*.ts`                                                                               | Service methods + inline spec / `flows/`                                  |
| 2   | Local functions inside spec files                                                                     | Inline code or service/flow methods                                       |
| 3   | Raw `fetch` / `context.get` in specs                                                                  | `activiti` fixture → service method                                       |
| 4   | BPMN/JSON as string templates in specs                                                                | `resources/modeling-projects/`                                            |
| 5   | `try/catch` or `try/finally` in specs                                                                 | Playwright hooks + `helpers/query-sync`                                   |
| 6   | JSDoc / inline comments on generated code                                                             | Self-documenting names                                                    |
| 7   | Cleanup after final `expect` in test body                                                             | `afterEach` / `afterAll` hooks                                            |
| 8   | `import { test } from '@playwright/test'`                                                             | Import `activiti`, `expect` from fixtures                                 |
| 9   | `expect` inside `try/catch`, `if`, `switch`, loops                                                    | Unconditional assertions                                                  |
| 10  | Multi-phase test without `activiti.step()`                                                            | Wrap each phase in `activiti.step()`                                      |
| 11  | WebSocket cleanup in test body                                                                        | `afterEach` with describe-scoped handle                                   |
| 12  | Wrong GraphQL WS subprotocol                                                                          | `graphql-transport-ws` via `services/notifications/`                      |
| 13  | Inline `activiti.skip()` without ticket                                                               | `pickScenarioTest` + Jira URL                                             |
| 14  | Hardcoded `/query/...` or `/audit/...` in specs                                                       | Instance `build*StatusChecks()` via fixture service                       |
| 15  | `services/*.helpers.ts`, free `build*` functions, or service class imports in specs for status checks | Shared on `BaseService`; builders as instance methods on fixture services |
