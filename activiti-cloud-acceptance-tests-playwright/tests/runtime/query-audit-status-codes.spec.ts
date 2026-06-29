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

const FAKE_ID = '00000000-0000-0000-0000-000000000099';
const LINK_TYPE = 'acceptance-link-type';

activiti.describe('Runtime — Query and Audit Status Codes', () => {
    activiti('should return 404 for missing query and audit resources', async ({
        queryServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        await activiti.step('When user query GET endpoints reference unknown ids', async () => {
            for (const { run } of queryServiceTestUser.buildNotFoundGetStatusChecks(FAKE_ID)) {
                expect(await run(queryServiceTestUser)).toBe(404);
            }
        });

        await activiti.step('When user query link targets an unknown process instance', async () => {
            for (const { run } of queryServiceTestUser.buildNotFoundPostStatusChecks(FAKE_ID, LINK_TYPE)) {
                expect(await run(queryServiceTestUser)).toBe(404);
            }
        });

        await activiti.step('When admin query GET endpoints reference unknown ids', async () => {
            for (const { run } of queryAdminServiceTestAdmin.buildNotFoundGetStatusChecks(FAKE_ID)) {
                expect(await run(queryAdminServiceTestAdmin)).toBe(404);
            }
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
            for (const { run } of queryServiceTestUser.buildBadRequestPostStatusChecks(FAKE_ID)) {
                expect(await run(queryServiceTestUser)).toBe(400);
            }
            for (const { run } of queryAdminServiceTestAdmin.buildBadRequestPostStatusChecks(FAKE_ID)) {
                expect(await run(queryAdminServiceTestAdmin)).toBe(400);
            }
        });

        await activiti.step('When process instance link body is invalid', async () => {
            for (const { run } of queryServiceTestUser.buildBadRequestLinkStatusChecks(
                processInstanceId,
                LINK_TYPE
            )) {
                expect(await run(queryServiceTestUser)).toBe(400);
            }
        });

        await activiti.step('When audit export date range is invalid', async () => {
            for (const { run } of auditAdminServiceTestAdmin.buildBadRequestGetStatusChecks()) {
                expect(await run(auditAdminServiceTestAdmin)).toBe(400);
            }
        });
    });

    activiti('should return 403 for unauthenticated POST query endpoints', async ({ anonymousQueryService }) => {
        for (const { label, run } of anonymousQueryService.buildUnauthenticatedPostStatusChecks(
            FAKE_ID,
            LINK_TYPE
        )) {
            await activiti.step(`POST query ${label}`, async () => {
                expect(await run(anonymousQueryService)).toBe(403);
            });
        }
    });

    activiti('should return 403 for unauthenticated POST query admin endpoints', async ({
        anonymousQueryAdminService,
    }) => {
        for (const { label, run } of anonymousQueryAdminService.buildUnauthenticatedPostStatusChecks(FAKE_ID)) {
            await activiti.step(`POST query admin ${label}`, async () => {
                expect(await run(anonymousQueryAdminService)).toBe(403);
            });
        }
    });

    activiti('should return 401 for invalid-token POST query endpoints', async ({
        invalidTokenQueryService,
    }) => {
        for (const { label, run } of invalidTokenQueryService.buildUnauthenticatedPostStatusChecks(
            FAKE_ID,
            LINK_TYPE
        )) {
            await activiti.step(`POST query ${label}`, async () => {
                expect(await run(invalidTokenQueryService)).toBe(401);
            });
        }
    });

    activiti('should return 401 for invalid-token POST query admin endpoints', async ({
        invalidTokenQueryAdminService,
    }) => {
        for (const { label, run } of invalidTokenQueryAdminService.buildUnauthenticatedPostStatusChecks(FAKE_ID)) {
            await activiti.step(`POST query admin ${label}`, async () => {
                expect(await run(invalidTokenQueryAdminService)).toBe(401);
            });
        }
    });

    activiti('should return 401 for unauthenticated GET query endpoints', async ({ anonymousQueryService }) => {
        for (const { label, run } of anonymousQueryService.buildUnauthenticatedGetStatusChecks(FAKE_ID)) {
            await activiti.step(`GET query ${label}`, async () => {
                expect(await run(anonymousQueryService)).toBe(401);
            });
        }
    });

    activiti('should return 401 for unauthenticated GET query admin endpoints', async ({
        anonymousQueryAdminService,
    }) => {
        for (const { label, run } of anonymousQueryAdminService.buildUnauthenticatedGetStatusChecks(FAKE_ID)) {
            await activiti.step(`GET query admin ${label}`, async () => {
                expect(await run(anonymousQueryAdminService)).toBe(401);
            });
        }
    });

    activiti('should return 401 for unauthenticated GET audit endpoints', async ({ anonymousAuditService }) => {
        for (const { label, run } of anonymousAuditService.buildUnauthenticatedGetStatusChecks(FAKE_ID)) {
            await activiti.step(`GET audit ${label}`, async () => {
                expect(await run(anonymousAuditService)).toBe(401);
            });
        }
    });

    activiti('should return 401 for unauthenticated GET audit admin endpoints', async ({
        anonymousAuditAdminService,
    }) => {
        for (const { label, run } of anonymousAuditAdminService.buildUnauthenticatedGetStatusChecks()) {
            await activiti.step(`GET audit admin ${label}`, async () => {
                expect(await run(anonymousAuditAdminService)).toBe(401);
            });
        }
    });

    activiti('should return 403 when the caller lacks query or audit permissions', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        auditServiceTestUser,
        auditServiceHrUser,
        auditAdminServiceHrUser,
        queryServiceTestUser,
        queryServiceHrUser,
        queryAdminServiceHrUser,
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
            for (const { run } of queryServiceHrUser.buildForbiddenGetStatusChecks(taskId)) {
                expect(await run(queryServiceHrUser)).toBe(403);
            }
            for (const { run } of auditServiceHrUser.buildForbiddenGetStatusChecks(eventId)) {
                expect(await run(auditServiceHrUser)).toBe(403);
            }
        });

        await activiti.step('When hruser calls admin query endpoints', async () => {
            for (const { run } of queryAdminServiceHrUser.buildForbiddenGetStatusChecks(taskId, processInstanceId)) {
                expect(await run(queryAdminServiceHrUser)).toBe(403);
            }
            for (const { run } of queryAdminServiceHrUser.buildForbiddenPostStatusChecks(taskId, FAKE_ID)) {
                expect(await run(queryAdminServiceHrUser)).toBe(403);
            }
        });

        await activiti.step('When hruser exports audit events via admin API', async () => {
            for (const { run } of auditAdminServiceHrUser.buildForbiddenGetStatusChecks()) {
                expect(await run(auditAdminServiceHrUser)).toBe(403);
            }
        });
    });
});
