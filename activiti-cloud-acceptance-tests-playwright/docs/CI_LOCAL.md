# CI vs local environment flow

Playwright tests resolve cluster URLs from the same env vars in CI and locally; only transport differs (HTTPS vs port-forward).

```mermaid
flowchart TD
    subgraph bootstrap [Bootstrap]
        A[config/bootstrap.ts] --> B[load-env.ts]
        B --> C{CI?}
        C -->|no| D[Read .env override=false]
        C -->|yes| E[Workflow env wins]
        A --> F[env-hosts.ts applyResolvedHostsToEnv]
    end

    subgraph resolved [Resolved env]
        F --> G[GATEWAY_HOST]
        F --> H[GATEWAY_URL]
        F --> I[IDENTITY_HOST]
    end

    subgraph consumers [Consumers]
        G --> J[test-configuration.ts baseURL / identityURL]
        H --> J
        I --> J
        J --> K[playwright.config.ts]
        J --> L[global-setup.ts port-forward]
    end

    subgraph ci [GitHub Actions only]
        M[export-playwright-ci-env.sh] --> N[PREVIEW_NAME CLUSTER_NAME matrix]
        N --> F
        O[prepare-preview-for-playwright.sh] --> P[Cluster overlay / health]
    end
```

## Key variables

| Variable                          | CI                                   | Local                  |
| --------------------------------- | ------------------------------------ | ---------------------- |
| `PREVIEW_NAME`                    | Matrix cell (`pr-123-rabbit-n-d`, …) | From `.env`            |
| `CLUSTER_NAME` / `CLUSTER_DOMAIN` | Workflow                             | `.env`                 |
| `GATEWAY_HOST`                    | Derived or explicit                  | Derived + `LOCAL_PORT` |
| `GATEWAY_URL`                     | `https://…`                          | `http://…:8080`        |
| Test users                        | GitHub vars/secrets `ACCEPTANCE_*`   | `.env`                 |

## Override polling in slow clusters

```bash
PLAYWRIGHT_POLL_QUERY_SYNC_MS=90000 npm test
```

Auth for tests uses worker-scoped `AuthCache`; setup/preflight uses `withAuthenticatedContext()` from `fixtures/auth-context.ts` (same `ContextFactory` path).
