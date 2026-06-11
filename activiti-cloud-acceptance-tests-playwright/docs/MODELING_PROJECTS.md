# Modeling projects (`resources/`)

BPMN and extension JSON for acceptance tests live under **`resources/modeling-projects/`**, similar to modeling assets in other Hyland repos (e.g. `hxp-studio-services`).

## Layout

```text
resources/modeling-projects/
└── acceptance/                          # Mounted on runtime-bundle when needed
    ├── HeadersConnectorProcess.bpmn20.xml
    └── HeadersConnectorProcess-extensions.json
```

Add a new subdirectory per logical project (e.g. `acceptance/`, future `smoke-only/`) rather than dropping files under `config/`.

## Runtime bundle image vs supplemental mount

| Image                                                      | Process catalog                                                                                         |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `activiti/example-runtime-bundle` (default for Playwright) | Full Serenity parity catalog on classpath — **no** supplemental mount                                   |
| Slim / custom chart RB image                               | May miss keys (e.g. `HeadersConnectorProcess`) — prereqs mount `resources/modeling-projects/acceptance` |

`npm run cluster:prereqs` / `test:setup`:

1. Builds ConfigMap `acceptance-supplemental-processes` from `resources/modeling-projects/acceptance/`
2. Mounts at `/config/acceptance-supplemental-processes/` on runtime-bundle **only when** `NEEDS_SUPPLEMENTAL_PROCESSES=1`
3. Skips mount when `ACCEPTANCE_RUNTIME_BUNDLE_IMAGE` contains `example-runtime-bundle` (default)

Override: `ACCEPTANCE_USE_SUPPLEMENTAL_PROCESSES=true|false` in `.env`.

## Security policies (not modeling projects)

`config/cluster/acceptance-security-policies.properties` stays under `config/cluster/` — it is cluster ConfigMap content, not BPMN.

## TypeScript paths

`config/paths.ts` exposes:

- `paths.modelingProjects.acceptance` — source directory for supplemental BPMN
- `paths.cluster.securityPoliciesFile` — acceptance policies properties file
