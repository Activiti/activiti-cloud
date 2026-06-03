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

| File                                          | Status                                                                                                               |
| --------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `notifications-actions.story` (6 scenarios)   | Playwright `tests/runtime/notifications.spec.ts` (Serenity kept until retirement ticket)                             |
| `delete-actions.story.disabled` (2 scenarios) | Playwright `tests/runtime/delete-actions.spec.ts` — audit ported (`destructive-last` project); query `activiti.skip` |

All other `runtime-bundle/*.story` files were removed after migration to Playwright (#2338).

Do not add new `.story` files here. See [../activiti-cloud-acceptance-tests-playwright/docs/AAE-46640.md](../activiti-cloud-acceptance-tests-playwright/docs/AAE-46640.md) and [SERENITY_RETIREMENT.md](../activiti-cloud-acceptance-tests-playwright/docs/SERENITY_RETIREMENT.md).
