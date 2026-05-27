# Deprecated — Serenity acceptance scenarios

This Maven module is **legacy**. New work belongs in:

`activiti-cloud-acceptance-tests-playwright/`

## Status

| Module | Replacement |
|--------|-------------|
| `security-policies-acceptance-tests` | `tests/*security-policies.spec.ts` |
| `runtime-acceptance-tests` (partial) | `tests/runtime/*.spec.ts` |
| `multiple-runtime-acceptance-tests` (partial) | `tests/process-instance-actions.spec.ts` |

Do not add new `.story` files here. See [../activiti-cloud-acceptance-tests-playwright/docs/SERENITY_RETIREMENT.md](../activiti-cloud-acceptance-tests-playwright/docs/SERENITY_RETIREMENT.md) for removal criteria.
