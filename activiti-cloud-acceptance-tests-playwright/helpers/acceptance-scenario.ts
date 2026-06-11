/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { activiti as ActivitiTest } from '../fixtures/services.fixture';

/**
 * Upstream-blocked acceptance scenarios: set `exclude` to keep the spec in the suite but
 * register it as Playwright-skipped (same pattern as process-instance-error-events-actions).
 */
export type AcceptanceScenarioMeta = {
    title: string;
    /** When set, scenario is excluded until the upstream issue is fixed. */
    exclude?: string;
};

/** Pick `test` or `test.skip` for a scenario (exclude = skipped, not failed). */
export function pickScenarioTest(
    test: typeof ActivitiTest,
    scenario: AcceptanceScenarioMeta
): typeof ActivitiTest {
    // test.skip is callable but narrows to TestType['skip']; cast keeps fixture typing.
    return (scenario.exclude ? test.skip : test) as typeof ActivitiTest;
}
