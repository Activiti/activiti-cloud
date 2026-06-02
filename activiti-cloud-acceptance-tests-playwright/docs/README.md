# Playwright acceptance — documentation

| Document                                         | Purpose                                                     |
| ------------------------------------------------ | ----------------------------------------------------------- |
| [../README.md](../README.md)                     | Quick start, commands, `.env` (start here)                  |
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)     | Code layout (`config/`, `fixtures/`, `services/`, …)        |
| [MODELING_PROJECTS.md](MODELING_PROJECTS.md)     | BPMN under `resources/modeling-projects/` and cluster mount |
| [PARALLEL_SAFE.md](PARALLEL_SAFE.md)             | Isolation rules for parallel workers                        |
| [SERENITY_RETIREMENT.md](SERENITY_RETIREMENT.md) | Serenity vs Playwright coverage and CI gate                 |
| [MIGRATION_PLAN.md](MIGRATION_PLAN.md)           | Story-by-story migration tracker (Serenity → Playwright)    |

Run `npm run typecheck` before pushing TypeScript changes.

All package documentation lives under `docs/` except the root [README.md](../README.md) entry point.
