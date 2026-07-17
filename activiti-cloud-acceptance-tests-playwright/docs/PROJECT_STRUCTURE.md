# Project structure

```text
activiti-cloud-acceptance-tests-playwright/
├── playwright.config.ts       # Playwright projects, reporters, global setup
├── README.md                  # Quick start (links here for detail)
├── docs/                      # All extended documentation
├── tests/                     # Spec files (*.spec.ts) grouped by domain folder
│   ├── identity/              # Identity adapter (Keycloak)
│   ├── security/              # HR user / HR admin security policies
│   ├── runtime-process/       # Process instance, definition, BPMN element specs
│   ├── runtime-task/          # Task and task-variable specs
│   └── runtime/               # Cross-cutting runtime (application, swagger, notifications, delete)
├── fixtures/
│   ├── context.models.ts      # CustomAPIRequest, token types
│   ├── context-factory.ts     # Keycloak + wrapAuthenticatedApiContext (Proxy)
│   ├── auth-cache.ts          # Worker-scoped token/context cache
│   ├── context.fixture.ts     # exports contexts, expect
│   └── services.fixture.ts    # exports activiti (services + cleanup)
├── services/                  # REST API clients (extend BaseService)
├── flows/
│   └── start-process-with-first-task.ts  # startCatalogProcess(), startCatalogProcessWithFirstTask()
├── helpers/                   # Test utilities, logging, assertions
│   ├── security-policies.assertions.ts
│   ├── multiple-runtime.assertions.ts
│   └── logging/               # Winston loggers for API + cleanup
├── models/                    # DTOs, ProcessDefinitionRegistry
├── config/
│   ├── paths.ts               # Package paths (.env, cluster assets, resources)
│   ├── users.ts               # Test user credentials from env
│   ├── load-env.ts            # dotenv entry
│   ├── cluster/               # K8s ConfigMap sources (security policies)
│   ├── connection/            # Gateway, SSO, port-forward, preview name
│   ├── runtime/               # Timeouts, test-configuration
│   ├── validation/            # Preflight / environment-validator
│   └── lifecycle/             # global-setup, global-teardown, setup/*
├── resources/
│   └── modeling-projects/     # BPMN + extensions (see MODELING_PROJECTS.md)
└── scripts/                   # test:setup, cluster:prereqs, matrix, port-forward
```

## Import direction

`tests` → `fixtures` → `helpers` / `services` → `models` / `config` / `fixtures`.

Tests should import `activiti` and `expect` from fixtures, not services directly.
