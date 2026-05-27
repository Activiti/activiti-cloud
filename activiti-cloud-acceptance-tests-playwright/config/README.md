# Playwright acceptance — configuration layout

```
config/
├── load-env.ts              # dotenv entry (import first)
├── cluster/                 # K8s ConfigMap sources (mounted by apply-cluster-prereqs.sh)
│   ├── acceptance-security-policies.properties
│   └── supplemental-processes/
├── connection/              # URLs, hosts, SSO, port-forward target
├── runtime/                 # Playwright timeouts + CI/local base URLs
├── validation/              # .env + preflight checks
└── lifecycle/               # Playwright global setup / teardown
    ├── global-setup.ts
    ├── global-teardown.ts
    ├── setup/               # kubeconfig, prereqs, port-forward, catalog
    └── teardown/
```

Cluster asset paths are also exposed on `paths.cluster` in `paths.ts`.
