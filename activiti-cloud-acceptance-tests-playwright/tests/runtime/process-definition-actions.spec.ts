/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { activiti, expect } from '../../fixtures/services.fixture';
import { pickHighestVersionByKey } from '../../helpers/process-definition';

const SINGLE_TASK_PROCESS = 'SingleTaskProcess';
const BIG_PROCESS = 'bigProcess';

// Mirrors XmlAssert ignoreWhitespace + node filter (drop <path>) + attr filter (drop style="...").
function normalizeSvg(svg: string): string {
    return svg
        .replace(/<path\b[^>]*\/>/g, '')
        .replace(/<path\b[^>]*>[\s\S]*?<\/path>/g, '')
        .replace(/\sstyle="[^"]*"/g, '')
        .replace(/>\s+</g, '><')
        .replace(/\s+/g, ' ')
        .trim();
}

activiti.describe('Process Definition Actions', () => {
    activiti('as a user I should be able to get process model', async ({ queryServiceTestUser }) => {
        await activiti.step(
            `Then the user can get the process model for process with key ${SINGLE_TASK_PROCESS} by passing its id`,
            async () => {
                const definitions = await queryServiceTestUser.getProcessDefinitions();
                const definition = pickHighestVersionByKey(definitions, SINGLE_TASK_PROCESS);

                const processModel = await queryServiceTestUser.getProcessModel(definition.id);

                expect(processModel).not.toBe('');
                expect(processModel).toContain(`bpmn2:process id="${SINGLE_TASK_PROCESS}"`);
            }
        );
    });

    activiti('as a user I should be able to get process diagram', async ({
        runtimeBundleServiceTestUser,
    }) => {
        await activiti.step(
            `Then the process diagram image for process with key ${BIG_PROCESS} is the same as process-definition-diagram.result.svg file`,
            async () => {
                const definition = await runtimeBundleServiceTestUser.getProcessDefinitionByKey(BIG_PROCESS);

                const processDiagram = await runtimeBundleServiceTestUser.getProcessDefinitionDiagram(
                    definition.id
                );
                const expected = readFileSync(
                    resolve(__dirname, '../../resources/runtime/process-definition-diagram.result.svg'),
                    'utf-8'
                );

                expect(normalizeSvg(processDiagram)).toBe(normalizeSvg(expected));
            }
        );
    });
});
