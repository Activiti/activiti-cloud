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

import { activiti, expect } from '../../fixtures/services.fixture';
import { startCatalogProcess } from '../../flows/start-catalog-process';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import { EventType } from '../../models/audit.models';
import { HttpStatusCheck } from '../../models/base-service.models';

const FAKE_ID = '00000000-0000-0000-0000-000000000099';
const LINK_TYPE = 'acceptance-link-type';

async function collectStatusCodes<TService>(
    checks: readonly HttpStatusCheck<TService>[],
    service: TService
): Promise<number[]> {
    return Promise.all(checks.map(({ run }) => run(service)));
}

activiti.describe('Runtime — Query and Audit Status Codes', () => {
    activiti('should return 404 for missing query and audit resources', async ({
        queryServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        await activiti.step('When user query GET endpoints reference unknown ids', async () => {
            const checks = queryServiceTestUser.buildNotFoundGetStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, queryServiceTestUser);
            expect(statuses).toEqual(checks.map(() => 404));
        });

        await activiti.step('When user query link targets an unknown process instance', async () => {
            const checks = queryServiceTestUser.buildNotFoundPostStatusChecks(FAKE_ID, LINK_TYPE);
            const statuses = await collectStatusCodes(checks, queryServiceTestUser);
            expect(statuses).toEqual(checks.map(() => 404));
        });

        await activiti.step('When admin query GET endpoints reference unknown ids', async () => {
            const checks = queryAdminServiceTestAdmin.buildNotFoundGetStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, queryAdminServiceTestAdmin);
            expect(statuses).toEqual(checks.map(() => 404));
        });
    });

    activiti('should return 400 for invalid query and audit requests', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
        queryAdminServiceTestAdmin,
        auditAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';

        await activiti.step('Given a synced process instance for link validation', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            processInstanceId = processInstance.id;
            await queryServiceTestUser.waitForProcessInstanceSynced(processInstanceId);
        });

        await activiti.step('When task search bodies omit required boolean fields', async () => {
            const userChecks = queryServiceTestUser.buildBadRequestPostStatusChecks(FAKE_ID);
            const adminChecks = queryAdminServiceTestAdmin.buildBadRequestPostStatusChecks(FAKE_ID);
            const statuses = [
                ...(await collectStatusCodes(userChecks, queryServiceTestUser)),
                ...(await collectStatusCodes(adminChecks, queryAdminServiceTestAdmin)),
            ];
            expect(statuses).toEqual([...userChecks, ...adminChecks].map(() => 400));
        });

        await activiti.step('When process instance link body is invalid', async () => {
            const checks = queryServiceTestUser.buildBadRequestLinkStatusChecks(processInstanceId, LINK_TYPE);
            const statuses = await collectStatusCodes(checks, queryServiceTestUser);
            expect(statuses).toEqual(checks.map(() => 400));
        });

        await activiti.step('When audit export date range is invalid', async () => {
            const checks = auditAdminServiceTestAdmin.buildBadRequestGetStatusChecks();
            const statuses = await collectStatusCodes(checks, auditAdminServiceTestAdmin);
            expect(statuses).toEqual(checks.map(() => 400));
        });
    });

    activiti('should return 403 for unauthenticated POST query endpoints', async ({ anonymousQueryService }) => {
        await activiti.step('POST query endpoints without auth', async () => {
            const checks = anonymousQueryService.buildUnauthenticatedPostStatusChecks(FAKE_ID, LINK_TYPE);
            const statuses = await collectStatusCodes(checks, anonymousQueryService);
            expect(statuses).toEqual(checks.map(() => 403));
        });
    });

    activiti('should return 403 for unauthenticated POST query admin endpoints', async ({
        anonymousQueryAdminService,
    }) => {
        await activiti.step('POST query admin endpoints without auth', async () => {
            const checks = anonymousQueryAdminService.buildUnauthenticatedPostStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, anonymousQueryAdminService);
            expect(statuses).toEqual(checks.map(() => 403));
        });
    });

    activiti('should return 401 for invalid-token POST query endpoints', async ({
        invalidTokenQueryService,
    }) => {
        await activiti.step('POST query endpoints with invalid token', async () => {
            const checks = invalidTokenQueryService.buildUnauthenticatedPostStatusChecks(FAKE_ID, LINK_TYPE);
            const statuses = await collectStatusCodes(checks, invalidTokenQueryService);
            expect(statuses).toEqual(checks.map(() => 401));
        });
    });

    activiti('should return 401 for invalid-token POST query admin endpoints', async ({
        invalidTokenQueryAdminService,
    }) => {
        await activiti.step('POST query admin endpoints with invalid token', async () => {
            const checks = invalidTokenQueryAdminService.buildUnauthenticatedPostStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, invalidTokenQueryAdminService);
            expect(statuses).toEqual(checks.map(() => 401));
        });
    });

    activiti('should return 401 for unauthenticated GET query endpoints', async ({ anonymousQueryService }) => {
        await activiti.step('GET query endpoints without auth', async () => {
            const checks = anonymousQueryService.buildUnauthenticatedGetStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, anonymousQueryService);
            expect(statuses).toEqual(checks.map(() => 401));
        });
    });

    activiti('should return 401 for unauthenticated GET query admin endpoints', async ({
        anonymousQueryAdminService,
    }) => {
        await activiti.step('GET query admin endpoints without auth', async () => {
            const checks = anonymousQueryAdminService.buildUnauthenticatedGetStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, anonymousQueryAdminService);
            expect(statuses).toEqual(checks.map(() => 401));
        });
    });

    activiti('should return 401 for unauthenticated GET audit endpoints', async ({ anonymousAuditService }) => {
        await activiti.step('GET audit endpoints without auth', async () => {
            const checks = anonymousAuditService.buildUnauthenticatedGetStatusChecks(FAKE_ID);
            const statuses = await collectStatusCodes(checks, anonymousAuditService);
            expect(statuses).toEqual(checks.map(() => 401));
        });
    });

    activiti('should return 401 for unauthenticated GET audit admin endpoints', async ({
        anonymousAuditAdminService,
    }) => {
        await activiti.step('GET audit admin endpoints without auth', async () => {
            const checks = anonymousAuditAdminService.buildUnauthenticatedGetStatusChecks();
            const statuses = await collectStatusCodes(checks, anonymousAuditAdminService);
            expect(statuses).toEqual(checks.map(() => 401));
        });
    });

    activiti('should return 403 when the caller lacks query or audit permissions', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        auditServiceTestUser,
        auditServiceHrUser,
        queryServiceTestUser,
        queryServiceHrUser,
        queryAdminServiceHrUser,
        auditAdminServiceHrUser,
    }) => {
        let taskId = '';
        let eventId = '';
        let processInstanceId = '';

        await activiti.step('Given a process instance owned by testuser with audit events', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            const event = await auditServiceTestUser.waitForEventOfTypeForProcessInstance(
                processInstance.id,
                EventType.PROCESS_STARTED
            );
            eventId = event.id;
            taskId = task.id;
            processInstanceId = processInstance.id;
            await queryServiceTestUser.waitForTaskById(taskId, () => true);
        });

        await activiti.step('When hruser reads restricted user query resources', async () => {
            const queryChecks = queryServiceHrUser.buildForbiddenGetStatusChecks(taskId);
            const auditChecks = auditServiceHrUser.buildForbiddenGetStatusChecks(eventId);
            const statuses = [
                ...(await collectStatusCodes(queryChecks, queryServiceHrUser)),
                ...(await collectStatusCodes(auditChecks, auditServiceHrUser)),
            ];
            expect(statuses).toEqual([...queryChecks, ...auditChecks].map(() => 403));
        });

        await activiti.step('When hruser calls admin query endpoints', async () => {
            const getChecks = queryAdminServiceHrUser.buildForbiddenGetStatusChecks(taskId, processInstanceId);
            const postChecks = queryAdminServiceHrUser.buildForbiddenPostStatusChecks(taskId, FAKE_ID);
            const statuses = [
                ...(await collectStatusCodes(getChecks, queryAdminServiceHrUser)),
                ...(await collectStatusCodes(postChecks, queryAdminServiceHrUser)),
            ];
            expect(statuses).toEqual([...getChecks, ...postChecks].map(() => 403));
        });

        await activiti.step('When hruser exports audit events via admin API', async () => {
            const checks = auditAdminServiceHrUser.buildForbiddenGetStatusChecks();
            const statuses = await collectStatusCodes(checks, auditAdminServiceHrUser);
            expect(statuses).toEqual(checks.map(() => 403));
        });
    });
});
