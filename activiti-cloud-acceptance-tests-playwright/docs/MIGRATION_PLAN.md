# Plan migracji Serenity → Playwright

Dokument opisuje **konkretną kolejność prac** z priorytetem na **lokalne uruchomienie** testów z komputera dewelopera/QA, a następnie na przejęcie gate’a CI.

**Stan (2026-05-27):**

| Stack              | Scenariusze / testy                 | Gate CI                                        |
| ------------------ | ----------------------------------- | ---------------------------------------------- |
| Serenity (JBehave) | ~117 scenariuszy w 3 modułach Maven | Legacy `runtime-acceptance-tests` (wycofywane) |
| Playwright         | **58** testów w 8 specach           | Pełna macierz messaging w `main.yml`           |

---

## Cel końcowy

- Zero zależności od Serenity w repozytorium.
- Pełna parity funkcjonalna (lub świadomie wycofane scenariusze).
- Jedna komenda lokalna: `npm run test:all` (lub `test:smoke` / `test:regression`).
- CI oparte wyłącznie na Playwright + matrix messaging (Rabbit/Kafka).

---

## Faza 0 — Lokalne uruchomienie

**Źródło prawdy:** [../README.md](../README.md) (nie duplikuj kroków tutaj).

Skrót:

```bash
npm install && npm run install:browsers
export ACTIVITI_KUBECONFIG=~/Downloads/activiti.yaml && npm run kube:use
npm run test:setup -- --install
npm run check:env && npm run test:all   # 58 testów
```

- Node **22 LTS**, preview Keycloak w namespace (`realm activiti`, client `activiti`).
- Port-forward: Traefik (`PORT_FORWARD_NAMESPACE=traefik`) — ustawiane w global-setup; `npm run port-forward` tylko do debugowania.
- Prereqs: `hostAliases`, polityki, opcjonalny mount BPMN z `resources/modeling-projects/acceptance/` — [MODELING_PROJECTS.md](MODELING_PROJECTS.md).

---

## Faza 1 — Stabilizacja i smoke (tydzień 1–2)

| #   | Zadanie                                                  | Priorytet |
| --- | -------------------------------------------------------- | --------- |
| 1.1 | Tag `@smoke` na identity + security (18 testów)          | ✅        |
| 1.2 | `npm run test:smoke`                                     | ✅        |
| 1.3 | Polling w `helpers/multiple-runtime.assertions.ts`       | ✅        |
| 1.4 | Usunąć `devices['Desktop Chrome']` — API-only config     | P1        |
| 1.5 | `forbidOnly` + `retries: 2` w CI                         | P1        |
| 1.6 | CI: dodać `test:security` + `test:process` obok identity | P1        |
| 1.7 | `upload-artifact` dla JUnit + HTML report                | P1        |
| 1.8 | Macierz story → spec (CSV/Confluence)                    | P1        |

---

## Faza 2 — Migracja runtime gate CI (tydzień 3–8)

**Kolejność plików `.story`** (wg wpływu na CI i liczby scenariuszy):

| Kolejność | Plik Serenity                                     | Scenariusze                                                                                 | Spec Playwright docelowy                                                                    |
| --------- | ------------------------------------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| 1         | `process-instance-actions.story`                  | 23 (**23 in Playwright**, no skips — requires `example-runtime-bundle` + `cluster:prereqs`) | `tests/runtime/process-instance.spec.ts`, `tests/runtime/process-instance-extended.spec.ts` |
| 2         | `task-actions.story` (fala 1: 10 najważniejszych) | 10                                                                                          | `tests/runtime/task.spec.ts` (**10 in Playwright**)                                         |
| 3         | `process-instance-service-tasks-actions.story`    | 10                                                                                          | `tests/runtime/service-tasks.spec.ts`                                                       |
| 4         | `task-actions.story` (fala 2: pozostałe)          | 21                                                                                          | rozszerzenie `task.spec.ts`                                                                 |
| 5         | `notifications-actions.story`                     | 6                                                                                           | `tests/runtime/notifications.spec.ts`                                                       |
| 6         | `process-instance-connectors-actions.story`       | 4                                                                                           | `tests/runtime/connectors.spec.ts`                                                          |
| 7         | `process-instance-message-actions.story`          | 4                                                                                           | `tests/runtime/messages.spec.ts`                                                            |
| 8         | `process-instance-timer-actions.story`            | 3                                                                                           | `tests/runtime/timers.spec.ts`                                                              |
| 9         | `process-instance-error-events-actions.story`     | 3                                                                                           | `tests/runtime/error-events.spec.ts`                                                        |
| 10        | Pozostałe pliki po 1–3 scenariusze                | ~15                                                                                         | `tests/runtime/*.spec.ts`                                                                   |

**Zasady migracji pojedynczego scenariusza:**

1. Odczytaj `.story` + kroki Java (`*Actions.java`, `*Steps.java`).
2. Zidentyfikuj reużywalny flow → wydziel do `flows/` (nie kopiuj HTTP w spec).
3. Napisz spec z `activiti.step()` zachowując nazwy kroków BDD.
4. Uruchom lokalnie: `npm run test -- <spec>`.
5. Oznacz w macierzy: `migrated` / `deferred` / `dropped`.

**Współdzielenie z Serenity:**

- Logika REST z `activiti-cloud-acceptance-tests-core` → port do `api/*.client.ts`.
- Nie uruchamiaj obu stacków w jednym PR bez uzasadnienia — unikaj duplikacji utrzymaniowej.

---

## Faza 3 — Moduły poza starym CI Serenity (tydzień 9–10)

| Moduł Serenity                       | Stan PW  | Akcja                                                                            |
| ------------------------------------ | -------- | -------------------------------------------------------------------------------- |
| `security-policies-acceptance-tests` | ~parity  | Już w PW — utrzymać w CI                                                         |
| `multiple-runtime-acceptance-tests`  | 1 test   | Zweryfikować **dwa** runtime bundle URL; poprawić `MultipleRuntimeBundleService` |
| `identity-adapter`                   | tylko PW | Już w CI                                                                         |

---

## Faza 4 — Przełączenie gate CI (tydzień 11–12)

1. Dual-run: Serenity + Playwright na matrix (2–4 tygodnie obserwacji).
2. Playwright jako **required check**; Serenity `continue-on-error`.
3. Usunąć `make test/runtime-acceptance-tests` z `main.yml`.
4. Playwright matrix: Rabbit/Kafka × konfiguracje (jak dziś Serenity).

---

## Faza 5 — Usunięcie Serenity (tydzień 13–14)

Checklist:

- [ ] Usunąć `activiti-cloud-acceptance-tests/` (Maven)
- [ ] Usunąć `activiti-cloud-acceptance-scenarios/`
- [ ] Root `pom.xml` — moduły i `skipAcceptanceTests`
- [ ] `Makefile` — target `test/runtime-acceptance-tests`
- [ ] `activiti-cloud-dependencies` — BOM acceptance-tests
- [ ] `grep serenity` = 0 wyników
- [ ] Dokumentacja tylko Playwright

---

## Struktura katalogów

Zobacz [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) i [MODELING_PROJECTS.md](MODELING_PROJECTS.md).

---

## Komendy — ściąga

```bash
# Z root repozytorium
npm install
npm run check:env              # walidacja środowiska
npm run test:identity          # smoke identity
npm run test:security          # security policies
npm run test:process           # multi-runtime signal
npm run test:all               # wszystko
npm run report                 # HTML report po teście

# Pojedynczy plik
npx playwright test activiti-cloud-acceptance-tests-playwright/tests/identity-adapter.spec.ts \
  --config=activiti-cloud-acceptance-tests-playwright/playwright.config.ts

# Debug
npm run test:identity:debug
```

---

## Śledzenie postępu migracji

| Obszar                           | Serenity scen. | PW testy | Status                           |
| -------------------------------- | -------------- | -------- | -------------------------------- |
| Identity adapter                 | 0              | 9        | **GOTOWE** (nowe)                |
| Security hruser                  | 6              | 7        | **GOTOWE**                       |
| Security hradmin                 | 4              | 2        | **GOTOWE** (skonsolidowane)      |
| Multi-runtime signal             | 1              | 1        | **W TRAKCIE** (weryfikacja 2 RB) |
| Runtime process-instance-actions | 23             | 21       | **GOTOWE**                       |
| Runtime task-actions (fala 1)    | 10             | 10       | **GOTOWE**                       |
| Runtime bundle (reszta)          | ~73            | 0        | **W TRAKCIE**                    |

Aktualizuj tabelę po każdej zmigrowanej fali.

---

## Ryzyka i mitigacje

| Ryzyko                       | Mitigacja                                               |
| ---------------------------- | ------------------------------------------------------- |
| Flaky timery / async procesy | `expect.poll`, dłuższy timeout tylko tam gdzie potrzeba |
| Brak HRADMIN w `.env`        | `check:env` + `.env.example`                            |
| `/etc/hosts` vs port-forward | Domyślnie localhost + Host header                       |
| Regresja przy 1:1 kopiowaniu | Macierz parity + review asercji                         |
| Długi CI                     | smoke na PR, regression na merge / nocny                |

---

## Następny krok po Fazie 0

Rozpocząć **Fazę 2, kolejność 1**: migracja `process-instance-actions.story` (23 scenariusze) — to jedyny blokujący swap gate’a CI.
