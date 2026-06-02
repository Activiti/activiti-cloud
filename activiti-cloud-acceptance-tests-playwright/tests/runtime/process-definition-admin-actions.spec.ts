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

import { activiti, expect } from '../../fixtures/services.fixture';
import { CloudProcessDefinition } from '../../models/process-definition.models';

const SINGLE_TASK_PROCESS = 'SingleTaskProcess';

// Mirrors ProcessDefinitionActions#getProcessDefinition — picks the highest appVersion match for a key.
function findProcessDefinitionByKey(
    definitions: CloudProcessDefinition[],
    key: string
): CloudProcessDefinition {
    const matches = definitions.filter((def) => def.key === key);
    if (matches.length === 0) {
        throw new Error(`No process definition found matching key ${key}`);
    }
    return matches.reduce((best, current) => {
        const bestVersion = parseInt(String(best.appVersion ?? '0'), 10);
        const currentVersion = parseInt(String(current.appVersion ?? '0'), 10);
        return currentVersion > bestVersion ? current : best;
    });
}

activiti.describe('Process Definition Admin Actions', () => {
    activiti('as an admin I should be able to get process model', async ({ queryAdminServiceHradmin }) => {
        await activiti.step(
            `Then the user, using the admin endpoint, can get the process model for process with key ${SINGLE_TASK_PROCESS} by passing its id`,
            async () => {
                const definitions = await queryAdminServiceHradmin.getAllProcessDefinitionsAdmin();
                const definition = findProcessDefinitionByKey(definitions, SINGLE_TASK_PROCESS);

                const processModel = await queryAdminServiceHradmin.getProcessModel(definition.id);

                expect(processModel).not.toBe('');
                expect(processModel).toContain(`bpmn2:process id="${SINGLE_TASK_PROCESS}"`);
            }
        );
    });
});
