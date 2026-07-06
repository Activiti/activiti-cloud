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

import { format, subDays, addDays } from 'date-fns';
import { activiti, expect } from '../../fixtures/services.fixture';
import { startCatalogProcess } from '../../flows/start-process-with-first-task';
import { EventType } from '../../models/audit.models';

activiti.describe('Runtime — Audit Event Actions', () => {
    activiti('should get audit event by id', async ({
        runtimeBundleServiceTestUser,
        auditServiceTestUser,
    }) => {
        let eventId = '';

        await activiti.step('Given a running process instance with audit events', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            const event = await auditServiceTestUser.waitForEventOfTypeForProcessInstance(
                processInstance.id,
                EventType.PROCESS_STARTED
            );
            eventId = event.id;
            expect(eventId).toBeTruthy();
        });

        await activiti.step('When the user fetches the event by id', async () => {
            const event = await auditServiceTestUser.events.getEventById(eventId);
            expect(event.id).toBe(eventId);
            expect(event.eventType).toBe(EventType.PROCESS_STARTED);
        });
    });

    activiti('should export audit events as CSV via admin API', async ({
        auditAdminServiceTestAdmin,
    }) => {
        const from = format(subDays(new Date(), 7), 'yyyy-MM-dd');
        const to = format(addDays(new Date(), 1), 'yyyy-MM-dd');
        const fileName = `pw-audit-export-${Date.now()}.csv`;

        await activiti.step('Given audit events exist in the preview namespace', async () => {
            const events = await auditAdminServiceTestAdmin.waitForAllEventsAdminCountGreaterThan(0);
            expect(events.length).toBeGreaterThan(0);
        });

        await activiti.step('When testadmin exports events for the date range', async () => {
            const csv = await auditAdminServiceTestAdmin.adminEvents.exportEvents(fileName, from, to);
            expect(csv.length).toBeGreaterThan(0);
            expect(csv).toContain('EVENTTYPE');
        });
    });
});
