# Deprecated — Serenity acceptance scenarios

This Maven module is **legacy**. New work belongs in:

`activiti-cloud-acceptance-tests-playwright/`

## Removed from Serenity (migrated to Playwright)

| Former Serenity asset                          | Playwright replacement                                                              |
| ---------------------------------------------- | ----------------------------------------------------------------------------------- |
| `security-policies-acceptance-tests` (module)  | `tests/hruser-security-policies.spec.ts`, `tests/hradmin-security-policies.spec.ts` |
| `multiple-runtime-acceptance-tests` (module)   | `tests/process-instance-actions.spec.ts`                                            |
| `process-instance-actions.story`               | `tests/runtime/process-instance*.spec.ts`                                           |
| `task-actions.story` (wave 1 + partial wave 2) | `tests/runtime/task*.spec.ts`                                                       |

## Still in Serenity (`runtime-acceptance-tests`)

Remaining `.story` files under `runtime-acceptance-tests/src/main/resources/stories/runtime-bundle/`, including trimmed `task-actions.story` (wave 2 scenarios not yet in Playwright).

Do not add new `.story` files here. See [../activiti-cloud-acceptance-tests-playwright/docs/SERENITY_RETIREMENT.md](../activiti-cloud-acceptance-tests-playwright/docs/SERENITY_RETIREMENT.md).
