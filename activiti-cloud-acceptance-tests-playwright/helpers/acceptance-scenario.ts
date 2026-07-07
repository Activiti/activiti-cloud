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

export type AcceptanceScenarioMeta = {
    title: string;
    exclude?: string;
};

type ActivitiTestBody = Parameters<typeof ActivitiTest>[1];

export function pickScenarioTest(
    test: typeof ActivitiTest,
    scenario: AcceptanceScenarioMeta
): typeof ActivitiTest {
    if (!scenario.exclude) {
        return test;
    }

    const registerSkipped = (title: string, _body: ActivitiTestBody) => {
        test(title, async () => {
            test.skip(true, scenario.exclude);
        });
    };

    return Object.assign(registerSkipped, test) as typeof ActivitiTest;
}
