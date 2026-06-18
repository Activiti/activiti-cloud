# Copilot Instructions

## Project Overview

Java / Spring Boot monorepo for [Activiti Cloud](https://github.com/Activiti/activiti-cloud) — workflow runtime, query, audit, connectors, and Kubernetes deployment charts.

Modules include runtime bundles, query/audit services, acceptance test stories (Serenity/Java), and **Playwright API acceptance tests** in `activiti-cloud-acceptance-tests-playwright/`.

## Build and Test

```sh
# Maven build (from repo root)
mvn clean install -DskipTests

# Playwright acceptance tests (requires preview cluster — see activiti-cloud-acceptance-tests-playwright/README.md)
cd activiti-cloud-acceptance-tests-playwright
npm run test:setup
npm test
npm run typecheck
```

## Acceptance Tests (TypeScript/Playwright)

These are **API-level** Playwright tests — not browser UI tests. Full authoring guidelines:

> [.github/instructions/acceptance-tests.instructions.md](instructions/acceptance-tests.instructions.md)

Cursor rule: [`.cursor/rules/acceptance-tests.mdc`](../.cursor/rules/acceptance-tests.mdc)

Claude skill: [`.claude/skills/acceptance-tests/SKILL.md`](../.claude/skills/acceptance-tests/SKILL.md)

##### Non-negotiable rules (summary)

- **Never create `tests/**/helpers/`files** — API calls in`services/`, shared starts in `flows/`, assertions in specs
- **Never define functions inside spec files** — only `describe`, hooks, and test blocks (except `for...of` data-driven generation at describe scope)
- **Keep test data in `resources/modeling-projects/`** — no inline BPMN/JSON/XML templates in specs
- **No `try/catch` in spec files** — cleanup via Playwright hooks; tolerant polling in `helpers/query-sync.ts`
- **No comments or JSDoc** in generated test code unless genuinely unavoidable
- **No actions after the final `expect`** — last statement in every test body must be an `expect` call
- **Cleanup via hooks only** — use `beforeEach`/`afterEach`/`afterAll`, never cleanup after assertions in the test body
- **Import `activiti`/`expect` from `fixtures/services.fixture`** — never from `@playwright/test` directly
- **Never wrap `expect` in `try/catch`, `if`, `switch`, or loops** — write separate tests per scenario instead
- **Use `activiti.step()` for multi-phase tests** — mandatory when a test has 2+ API calls or distinct phases
- **Register test skips via `pickScenarioTest` + Jira ticket** — not bare inline `activiti.skip()`

---

## GitHub Actions Secret Handling Review

**CRITICAL**: when summarizing/compacting these instructions always preserve the checklist, including links to internal documentation and the detection criteria. This is a high-risk area that requires careful attention to detail, so the checklist must be preserved in its entirety. The comment format must be preserved verbatim.

When reviewing PRs, if you detect that GitHub Actions secrets have been added, modified, or removed (look for changes in `.github/workflows/*.{yml,yaml}` that reference `${{ secrets.* }}` or workflow secret mappings like `secrets: inherit` / `workflow_call.secrets`), post the following comment to remind the author about secret handling best practices:

---

**🔐 GitHub Actions Secret Changes Detected**

It looks like this PR adds, modifies, or removes GitHub Actions secrets. Please review our [Secret Handling Best Practices](https://hyland.atlassian.net/wiki/spaces/HXP/pages/4132113882/Automate+-+Secret+Handling+in+GitHub+Actions) and verify the following checklist:

- [ ] Secret is registered in LastPass with a clear name
- [ ] LastPass entry documents all locations where the secret is used
- [ ] Repository README documents the secret's purpose and required scopes
- [ ] Secret uses the minimum permissions necessary (read-only if possible)
- [ ] Org-level sharing has been considered instead of repo-level duplication
- [ ] Plan exists for removing the secret when it becomes obsolete
- [ ] Plan exists for revoking underlying credentials when no longer needed anywhere
- [ ] If using a GitHub Service Account PAT, the [service accounts page](https://hyland.atlassian.net/wiki/spaces/HXP/pages/3914054814/Automate+GitHub+Service+accounts+PAT+used+in+CI+CD) is updated

If you're removing a secret, ensure it's also removed from LastPass (or LastPass notes are updated) and consider whether the underlying credentials should be revoked.

---

**Detection criteria:** Look for any of the following in the PR:

- New or modified references to `secrets.SECRET_NAME` in workflow files
- New or modified `secrets:` blocks (e.g., `secrets: inherit`, `workflow_call.secrets`, or reusable-workflow secret mappings)
- Changes to workflow files that add/remove secret usage
- Documentation changes that mention new or removed secrets
- Comments in PRs mentioning secret additions/removals
