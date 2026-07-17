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
import { normalizeSvg } from '../../helpers/diagram-utils';

const SINGLE_TASK_PROCESS = 'SingleTaskProcess';
const BIG_PROCESS = 'bigProcess';
const PROCESS_WITH_VARIABLES = 'ProcessWithVariables';

activiti.describe('Process Definition Actions', () => {
    activiti('as a user I should be able to get process model', async ({ queryServiceTestUser }) => {
        await activiti.step(
            `Then the user can get the process model for process with key ${SINGLE_TASK_PROCESS} by passing its id`,
            async () => {
                const definition = await queryServiceTestUser.getProcessDefinitionByKey(SINGLE_TASK_PROCESS);

                const processModel = await queryServiceTestUser.processInstances.getProcessModel(definition.id);

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

                const processDiagram = await runtimeBundleServiceTestUser.processDefinitions.getProcessDefinitionDiagram(
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

    activiti('should cover RB process definition metadata, home, and connector endpoints', async ({
        runtimeBundleServiceTestUser,
        runtimeAdminServiceTestAdmin,
    }) => {
        let processDefinitionId = '';
        let connectorDefinitionId = '';

        await activiti.step('When the user fetches RB home info', async () => {
            const home = await runtimeBundleServiceTestUser.openApiSpec.getHomeInfo();
            expect(home).toBeTruthy();
        });

        await activiti.step('And resolves a deployed process definition', async () => {
            const definition = await runtimeBundleServiceTestUser.getProcessDefinitionByKey(PROCESS_WITH_VARIABLES);
            processDefinitionId = definition.id;
            expect(processDefinitionId).toBeTruthy();
        });

        await activiti.step('Then the user can fetch definition by id, meta, and mapping values', async () => {
            const byId = await runtimeBundleServiceTestUser.processDefinitions.getProcessDefinitionById(processDefinitionId);
            expect(byId.id).toBe(processDefinitionId);

            const meta = await runtimeBundleServiceTestUser.processDefinitions.getProcessDefinitionMeta(processDefinitionId);
            expect(Array.isArray(meta.userTasks)).toBe(true);
            expect((meta.userTasks ?? []).length).toBeGreaterThan(0);

            const staticValues =
                await runtimeBundleServiceTestUser.processDefinitions.getProcessDefinitionStaticValues(processDefinitionId);
            const constantValues =
                await runtimeBundleServiceTestUser.processDefinitions.getProcessDefinitionConstantValues(processDefinitionId);
            expect(staticValues).toEqual(expect.any(Object));
            expect(constantValues).toEqual(expect.any(Object));
        });

        await activiti.step('When the user lists connector definitions', async () => {
            const connectors = await runtimeBundleServiceTestUser.connectorDefinitions.getConnectorDefinitions();
            expect(Array.isArray(connectors)).toBe(true);
            expect(connectors.length).toBeGreaterThan(0);
            connectorDefinitionId = connectors[0].id;
        });

        await activiti.step('Then the user fetches a connector definition by id', async () => {
            const connector = await runtimeBundleServiceTestUser.connectorDefinitions.getConnectorDefinitionById(connectorDefinitionId);
            expect(connector.id).toBe(connectorDefinitionId);
        });

        await activiti.step('And the admin lists process definitions', async () => {
            const definitions = await runtimeAdminServiceTestAdmin.processDefinitions.getProcessDefinitions();
            expect(definitions.map((definition) => definition.key)).toContain(PROCESS_WITH_VARIABLES);
        });
    });
});
