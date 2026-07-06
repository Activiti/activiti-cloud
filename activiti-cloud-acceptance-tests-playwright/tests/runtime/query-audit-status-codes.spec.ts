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
import { startCatalogProcess } from '../../flows/start-process-with-first-task';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import { EventType } from '../../models/audit.models';

const FAKE_ID = '00000000-0000-0000-0000-000000000099';
const LINK_TYPE = 'acceptance-link-type';

activiti.describe('Runtime — Query and Audit Status Codes', () => {
    activiti('should return 404 for missing query and audit resources', async ({
        queryServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        await activiti.step('When user query GET endpoints reference unknown ids', async () => {
            const checks = queryServiceTestUser.statusChecks.buildNotFoundGetStatusChecks(FAKE_ID);
            const statuses = await queryServiceTestUser.runStatusChecks(checks);
            expect(statuses).toEqual(checks.map(() => 404));
        });

        await activiti.step('When user query link targets an unknown process instance', async () => {
            const checks = queryServiceTestUser.statusChecks.buildNotFoundPostStatusChecks(FAKE_ID, LINK_TYPE);
            const statuses = await queryServiceTestUser.runStatusChecks(checks);
            expect(statuses).toEqual(checks.map(() => 404));
        });

        await activiti.step('When admin query GET endpoints reference unknown ids', async () => {
            const checks = queryAdminServiceTestAdmin.statusChecks.buildNotFoundGetStatusChecks(FAKE_ID);
            const statuses = await queryAdminServiceTestAdmin.runStatusChecks(checks);
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
            const userChecks = queryServiceTestUser.statusChecks.buildBadRequestPostStatusChecks(FAKE_ID);
            const adminChecks = queryAdminServiceTestAdmin.statusChecks.buildBadRequestPostStatusChecks(FAKE_ID);
            const statuses = [
                ...(await queryServiceTestUser.runStatusChecks(userChecks)),
                ...(await queryAdminServiceTestAdmin.runStatusChecks(adminChecks)),
            ];
            expect(statuses).toEqual([...userChecks, ...adminChecks].map(() => 400));
        });

        await activiti.step('When process instance link body is invalid', async () => {
            const checks = queryServiceTestUser.statusChecks.buildBadRequestLinkStatusChecks(processInstanceId, LINK_TYPE);
            const statuses = await queryServiceTestUser.runStatusChecks(checks);
            expect(statuses).toEqual(checks.map(() => 400));
        });

        await activiti.step('When audit export date range is invalid', async () => {
            const checks = auditAdminServiceTestAdmin.statusChecks.buildBadRequestGetStatusChecks();
            const statuses = await auditAdminServiceTestAdmin.runStatusChecks(checks);
            expect(statuses).toEqual(checks.map(() => 400));
        });
    });

    activiti('should return 403 for unauthenticated POST query endpoints', async ({ anonymousQueryService }) => {
        const checks = anonymousQueryService.statusChecks.buildUnauthenticatedPostStatusChecks(FAKE_ID, LINK_TYPE);
        const statuses = await anonymousQueryService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 403));
    });

    activiti('should return 403 for unauthenticated POST query admin endpoints', async ({
        anonymousQueryAdminService,
    }) => {
        const checks = anonymousQueryAdminService.statusChecks.buildUnauthenticatedPostStatusChecks(FAKE_ID);
        const statuses = await anonymousQueryAdminService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 403));
    });

    activiti('should return 401 for invalid-token POST query endpoints', async ({ invalidTokenQueryService }) => {
        const checks = invalidTokenQueryService.statusChecks.buildUnauthenticatedPostStatusChecks(FAKE_ID, LINK_TYPE);
        const statuses = await invalidTokenQueryService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 401));
    });

    activiti('should return 401 for invalid-token POST query admin endpoints', async ({
        invalidTokenQueryAdminService,
    }) => {
        const checks = invalidTokenQueryAdminService.statusChecks.buildUnauthenticatedPostStatusChecks(FAKE_ID);
        const statuses = await invalidTokenQueryAdminService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 401));
    });

    activiti('should return 401 for unauthenticated GET query endpoints', async ({ anonymousQueryService }) => {
        const checks = anonymousQueryService.statusChecks.buildUnauthenticatedGetStatusChecks(FAKE_ID);
        const statuses = await anonymousQueryService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 401));
    });

    activiti('should return 401 for unauthenticated GET query admin endpoints', async ({
        anonymousQueryAdminService,
    }) => {
        const checks = anonymousQueryAdminService.statusChecks.buildUnauthenticatedGetStatusChecks(FAKE_ID);
        const statuses = await anonymousQueryAdminService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 401));
    });

    activiti('should return 401 for unauthenticated GET audit endpoints', async ({ anonymousAuditService }) => {
        const checks = anonymousAuditService.statusChecks.buildUnauthenticatedGetStatusChecks(FAKE_ID);
        const statuses = await anonymousAuditService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 401));
    });

    activiti('should return 401 for unauthenticated GET audit admin endpoints', async ({
        anonymousAuditAdminService,
    }) => {
        const checks = anonymousAuditAdminService.statusChecks.buildUnauthenticatedGetStatusChecks();
        const statuses = await anonymousAuditAdminService.runStatusChecks(checks);
        expect(statuses).toEqual(checks.map(() => 401));
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
            const queryChecks = queryServiceHrUser.statusChecks.buildForbiddenGetStatusChecks(taskId);
            const auditChecks = auditServiceHrUser.statusChecks.buildForbiddenGetStatusChecks(eventId);
            const statuses = [
                ...(await queryServiceHrUser.runStatusChecks(queryChecks)),
                ...(await auditServiceHrUser.runStatusChecks(auditChecks)),
            ];
            expect(statuses).toEqual([...queryChecks, ...auditChecks].map(() => 403));
        });

        await activiti.step('When hruser calls admin query endpoints', async () => {
            const getChecks = queryAdminServiceHrUser.statusChecks.buildForbiddenGetStatusChecks(taskId, processInstanceId);
            const postChecks = queryAdminServiceHrUser.statusChecks.buildForbiddenPostStatusChecks(taskId, FAKE_ID);
            const statuses = [
                ...(await queryAdminServiceHrUser.runStatusChecks(getChecks)),
                ...(await queryAdminServiceHrUser.runStatusChecks(postChecks)),
            ];
            expect(statuses).toEqual([...getChecks, ...postChecks].map(() => 403));
        });

        await activiti.step('When hruser exports audit events via admin API', async () => {
            const checks = auditAdminServiceHrUser.statusChecks.buildForbiddenGetStatusChecks();
            const statuses = await auditAdminServiceHrUser.runStatusChecks(checks);
            expect(statuses).toEqual(checks.map(() => 403));
        });
    });
});
