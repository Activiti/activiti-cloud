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
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { startCatalogProcess } from '../../flows/start-catalog-process';
import { pollOptions } from '../../config/runtime/timeouts';
import { isDiagramEmpty, isDiagramShown } from '../../helpers/diagram-utils';
import { RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS } from '../../helpers/process-deployment';

activiti.describe('Runtime — Process Instance Actions', () => {
    activiti('should delete a process instance', async ({ runtimeBundleServiceTestUser }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            expect(processInstanceId).toBeTruthy();
        });

        await activiti.step('And the user deletes the process', async () => {
            await runtimeBundleServiceTestUser.deleteProcessInstance(processInstanceId);
        });

        await activiti.step('Then the process instance is deleted', async () => {
            await expect(async () => {
                await runtimeBundleServiceTestUser.getProcessInstance(processInstanceId);
            }).rejects.toThrow();
        });
    });

    activiti('should resume a suspended process instance', async ({ runtimeBundleServiceTestUser }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            expect(processInstanceId).toBeTruthy();
        });

        await activiti.step('And the user suspends the process instance', async () => {
            const suspended = await runtimeBundleServiceTestUser.suspendProcessInstance(processInstanceId);
            expect(suspended.status).toBe(ProcessInstanceStatus.SUSPENDED);
        });

        await activiti.step('Then the status of the process is changed to suspended', async () => {
            await expect
                .poll(async () => {
                    const instance = await runtimeBundleServiceTestUser.getProcessInstance(processInstanceId);
                    return instance.status;
                }, pollOptions('processStatus'))
                .toBe(ProcessInstanceStatus.SUSPENDED);
        });

        await activiti.step('And the user is able to resume the process instance', async () => {
            await runtimeBundleServiceTestUser.resumeProcessInstance(processInstanceId);
        });

        await activiti.step('Then the status of the process is changed to running', async () => {
            await expect
                .poll(async () => {
                    const instance = await runtimeBundleServiceTestUser.getProcessInstance(processInstanceId);
                    return instance.status;
                }, pollOptions('processStatus'))
                .toBe(ProcessInstanceStatus.RUNNING);
        });
    });

    activiti('should not resume a deleted process instance', async ({ runtimeBundleServiceTestUser }) => {
        let processInstanceId: string;

        await activiti.step('Given any suspended process instance', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            await runtimeBundleServiceTestUser.suspendProcessInstance(processInstanceId);
        });

        await activiti.step('When the user deletes the process', async () => {
            await runtimeBundleServiceTestUser.deleteProcessInstance(processInstanceId);
        });

        await activiti.step('Then the process cannot be activated anymore', async () => {
            await expect(
                runtimeBundleServiceTestUser.resumeProcessInstance(processInstanceId)
            ).rejects.toThrow(/Unable to find process instance/);
        });
    });

    activiti('should show all process definitions on runtime and query', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        await activiti.step('When the user gets the process definitions', async () => {
            const runtimeDefinitions = await runtimeBundleServiceTestUser.getProcessDefinitions();
            const queryDefinitions = await queryServiceTestUser.getProcessDefinitions();
            const runtimeKeys = runtimeDefinitions.map((def) => def.key);
            const queryKeys = queryDefinitions.map((def) => def.key);

            expect(runtimeKeys).toEqual(expect.arrayContaining([...RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS]));
            expect(queryKeys).toEqual(expect.arrayContaining([...RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS]));
        });
    });

    activiti('should show a process instance diagram', async ({ runtimeBundleServiceTestUser }) => {
        let processInstanceId: string;
        let diagram: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('And open the process diagram', async () => {
            diagram = await runtimeBundleServiceTestUser.getProcessInstanceDiagram(processInstanceId);
        });

        await activiti.step('Then the diagram is shown', async () => {
            expect(isDiagramShown(diagram)).toBe(true);
        });
    });

    activiti('should not show diagram for a process instance without graphic info', async ({
        runtimeBundleServiceTestUser,
    }) => {
        let processInstanceId: string;
        let diagram: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO'
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('And open the process diagram', async () => {
            diagram = await runtimeBundleServiceTestUser.getProcessInstanceDiagram(processInstanceId);
        });

        await activiti.step('Then no diagram is shown', async () => {
            expect(isDiagramEmpty(diagram)).toBe(true);
        });
    });

    activiti('should show query process instance diagram for connector process', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let diagram: string;

        await activiti.step('When the user starts an instance of the process called CONNECTOR_PROCESS_INSTANCE', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'CONNECTOR_PROCESS_INSTANCE'
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('And query the process diagram', async () => {
            await expect
                .poll(
                    async () => {
                        diagram = await queryServiceTestUser.getProcessInstanceDiagram(processInstanceId);
                        return isDiagramShown(diagram);
                    },
                    pollOptions('querySync')
                )
                .toBe(true);
        });

        await activiti.step('Then the query diagram is shown', async () => {
            expect(isDiagramShown(diagram)).toBe(true);
        });
    });

    activiti('should show query diagram for process without graphic info', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let diagram: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO'
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('And query the process diagram', async () => {
            diagram = await queryServiceTestUser.getProcessInstanceDiagram(processInstanceId);
        });

        await activiti.step('Then the diagram is shown', async () => {
            expect(isDiagramShown(diagram)).toBe(true);
        });
    });

    activiti('should show query diagram in admin endpoint as process admin', async ({
        runtimeBundleServiceTestUser,
        queryAdminServiceProcessAdmin,
    }) => {
        let processInstanceId: string;
        let diagram: string;

        await activiti.step('When the user starts an instance of the process called CONNECTOR_PROCESS_INSTANCE', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'CONNECTOR_PROCESS_INSTANCE'
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('And query the process diagram admin endpoint', async () => {
            diagram = await queryAdminServiceProcessAdmin.getProcessInstanceDiagram(processInstanceId);
        });

        await activiti.step('Then the query diagram is shown in admin endpoint', async () => {
            expect(isDiagramShown(diagram)).toBe(true);
        });
    });
});
