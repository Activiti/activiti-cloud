---
name: pre-commit
description: >-
  Validate code quality and formatting before committing changes in activiti-cloud.
  Use whenever you have created or modified files and are about to call report_progress/commit.
  Triggers on: commit, pre-commit, formatting, prettier, trailing whitespace, end of file.
---

# Pre-commit Validation

This repository uses [pre-commit](https://pre-commit.com) hooks (see `.pre-commit-config.yaml`).
**Always run these checks on your changed files before calling `report_progress`** to avoid CI failures.

## Quick-start

Install pre-commit once (requires Python):

```bash
pip install pre-commit
pre-commit install
```

Run against only your changed files:

```bash
pre-commit run --files <path/to/changed/file1> <path/to/changed/file2>
```

Run against all staged/changed files at once:

```bash
pre-commit run --all-files
```

## Hooks that run on every commit

| Hook                     | What it checks                             | Applies to                                                |
| ------------------------ | ------------------------------------------ | --------------------------------------------------------- |
| `check-merge-conflict`   | No unresolved merge conflict markers       | All files                                                 |
| `fix-byte-order-marker`  | Removes UTF-8 BOM                          | All files                                                 |
| `mixed-line-ending`      | Converts to LF line endings                | All files                                                 |
| `end-of-file-fixer`      | Ensures files end with a newline           | All (except `*.lock.yml`, `*.md` in `.github/workflows/`) |
| `trailing-whitespace`    | Removes trailing spaces/tabs               | All (except `*.lock.yml`, `*.md` in `.github/workflows/`) |
| `check-yaml`             | Valid YAML                                 | `*.yaml`, `*.yml`                                         |
| `check-json`             | Valid JSON                                 | `*.json` (except files matching `*invalid*.json`)         |
| `check-xml`              | Valid XML                                  | `*.xml`                                                   |
| `prettier`               | Consistent formatting                      | Java (`*.java`) and Markdown (`*.md`)                     |
| `check-dependabot`       | Valid Dependabot v2 config                 | `.github/dependabot.yml`                                  |
| `check-github-actions`   | Valid GitHub Actions syntax                | `.github/actions/**`                                      |
| `check-github-workflows` | Valid GitHub Workflows syntax              | `.github/workflows/**` (except `*.lock.yml`)              |
| `semgrep`                | Static analysis rules from `.semgrep.yaml` | Java and XML                                              |

## Common failures and fixes

### prettier (Java formatting)

Prettier reformats Java files according to `prettier-plugin-java`. If it fails:

- The hook will **auto-fix** the file (CI uses auto-commit)
- Locally: run `pre-commit run prettier --files <file>` to apply the fix
- **Never commit syntactically invalid Java** — prettier cannot parse it and will fail

### trailing-whitespace / end-of-file-fixer

These hooks auto-fix. Locally run `pre-commit run trailing-whitespace --files <file>`.

### semgrep

Check `.semgrep.yaml` for the specific rule that was violated.

## Checklist before every `report_progress` call

- [ ] All Java files have matching braces (no missing `{` / `}`)
- [ ] No trailing whitespace on any changed line
- [ ] All changed files end with a single newline
- [ ] No `<<<<<<< HEAD` / `=======` / `>>>>>>>` conflict markers
- [ ] YAML/JSON/XML files are syntactically valid
