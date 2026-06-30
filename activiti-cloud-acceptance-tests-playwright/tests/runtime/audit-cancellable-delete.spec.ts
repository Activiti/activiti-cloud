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

/**
 * Acceptance tests for the cancellable async audit deletion API (AAE-47805).
 * Requires feature flag activiti.features.audit.cancellable-delete.enabled=true.
 * Tagged @destructive — runs last (project destructive-last, after notifications).
 */

import { activiti, expect } from '../../fixtures/services.fixture';
import { startCatalogProcess } from '../../flows/start-catalog-process';
import { AuditEventsDeletionStatus } from '../../models/audit.models';

activiti.describe('Audit — Cancellable Delete [AAE-47805]', { tag: ['@slow', '@destructive'] }, () => {
    activiti.describe.configure({ mode: 'serial' });

    activiti(
        'admin can start async audit events deletion and it completes successfully',
        async ({ runtimeBundleServiceTestUser, auditAdminServiceTestAdmin }) => {
            await activiti.step(
                'Given a process is started and audit events are synced',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                    );
                    expect(processInstance.id).toBeTruthy();
                    const events = await auditAdminServiceTestAdmin.waitForAllEventsAdminCountGreaterThan(0);
                    expect(events.length).toBeGreaterThan(0);
                }
            );

            await activiti.step(
                'When admin starts the cancellable deletion',
                async () => {
                    const startResponse = await auditAdminServiceTestAdmin.startCancellableDeletionAdmin();
                    expect(startResponse.message).toBeTruthy();
                    expect(startResponse.totalCount).toBeGreaterThan(0);
                    expect(startResponse.percentComplete).toBeGreaterThanOrEqual(0);
                }
            );

            await activiti.step(
                'Then deletion reaches a completed state and all audit events are removed',
                async () => {
                    const finalStatus = await auditAdminServiceTestAdmin.waitForDeletionCompletedAdmin();
                    expect(finalStatus.status).toBe(AuditEventsDeletionStatus.COMPLETED);
                    expect(finalStatus.percentComplete).toBe(100);
                    const events = await auditAdminServiceTestAdmin.waitForAllEventsAdminCount(0);
                    expect(events.length).toBe(0);
                }
            );
        }
    );

    activiti(
        'admin can query the deletion status while async deletion is running',
        async ({ runtimeBundleServiceTestUser, auditAdminServiceTestAdmin }) => {
            await activiti.step(
                'Given a process is started and audit events are synced',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                    );
                    expect(processInstance.id).toBeTruthy();
                    const events = await auditAdminServiceTestAdmin.waitForAllEventsAdminCountGreaterThan(0);
                    expect(events.length).toBeGreaterThan(0);
                }
            );

            await activiti.step('When admin starts the cancellable deletion', async () => {
                const startResponse = await auditAdminServiceTestAdmin.startCancellableDeletionAdmin();
                expect(startResponse.message).toBeTruthy();
            });

            await activiti.step(
                'Then the deletion status endpoint returns a valid response with progress fields',
                async () => {
                    const statusResponse = await auditAdminServiceTestAdmin.getDeletionStatusAdmin();
                    expect(statusResponse.status).toBeDefined();
                    expect(statusResponse.totalCount).toBeGreaterThanOrEqual(0);
                    expect(statusResponse.deletedCount).toBeGreaterThanOrEqual(0);
                    expect(statusResponse.remainingCount).toBeGreaterThanOrEqual(0);
                    expect(statusResponse.percentComplete).toBeGreaterThanOrEqual(0);
                }
            );
        }
    );
});
