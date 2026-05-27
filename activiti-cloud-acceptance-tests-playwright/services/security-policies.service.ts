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

import { RuntimeBundleService } from './runtime-bundle.service';
import { TaskService } from './task.service';
import { QueryService } from './query.service';
import { AuditService } from './audit.service';
import { AuditAdminService } from './audit-admin.service';
import { RuntimeAdminService } from './runtime-admin.service';
import { QueryAdminService } from './query-admin.service';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { CloudTask } from '../models/task.models';
import { CloudRuntimeEvent } from '../models/audit.models';
import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { expect } from '@playwright/test';
import { pollOptions } from '../config/runtime/timeouts';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../context.models';
import { DirtyContextRegistry } from '../helpers/dirty-context';
import { TestScope } from '../helpers/test-isolation';

export class SecurityPoliciesService extends BaseService {
    private readonly runtimeBundleService: RuntimeBundleService;
    private readonly taskService: TaskService;
    private readonly queryService: QueryService;
    private readonly auditService: AuditService;
    private readonly auditAdminService: AuditAdminService;
    private readonly runtimeAdminService: RuntimeAdminService;
    private readonly queryAdminService: QueryAdminService;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.runtimeBundleService = new RuntimeBundleService(context);
        this.taskService = new TaskService(context);
        this.queryService = new QueryService(context);
        this.auditService = new AuditService(context);
        this.auditAdminService = new AuditAdminService(context);
        this.runtimeAdminService = new RuntimeAdminService(context);
        this.queryAdminService = new QueryAdminService(context);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope): void {
        super.attachIsolation(dirtyRegistry, testScope);
        this.runtimeBundleService.attachIsolation(dirtyRegistry, testScope);
        this.taskService.attachIsolation(dirtyRegistry, testScope);
    }

    // Process Instance Operations
    async startProcess(processName: string): Promise<CloudProcessInstance> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        const startPath =
            this.context.username === 'hradmin' || this.context.username === 'processadmin'
                ? '/rb/admin/v1/process-instances'
                : '/rb/v1/process-instances';

        const response = await this.post(
            startPath,
            {
                data: {
                    payloadType: 'StartProcessPayload',
                    processDefinitionKey
                }
            }
        );

        if (response.httpStatus && response.httpStatus >= 400 && response.httpStatus < 500) {
            const entry = (response as any).entry;
            const errorMessage =
                entry?.message ||
                (response as any).message ||
                response.body ||
                `Unable to find process definition for the given id:'${processDefinitionKey}'`;
            throw new Error(errorMessage);
        }

        const entry = (response as any).entry;
        if (entry?.code && entry?.message) {
            throw new Error(entry.message);
        }

        const processInstance = this.unwrapEntity<CloudProcessInstance>(response);
        if (processInstance.id) {
            this.dirtyRegistry?.register(this.context, `${startPath}/${processInstance.id}`);
        }
        return processInstance;
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return await this.runtimeBundleService.getProcessInstances();
    }

    async getRuntimeProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.runtimeBundleService.getProcessInstance(processInstanceId);
    }

    async getProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        return await this.runtimeAdminService.getAllProcessInstances();
    }

    // Query Operations
    async queryAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return await this.queryService.getAllProcessInstances();
    }

    async queryAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        return await this.queryAdminService.getAllProcessInstancesAdmin();
    }

    // Task Operations
    async getAllTasks(): Promise<CloudTask[]> {
        return await this.taskService.getAllTasks();
    }

    async queryAllTasks(): Promise<CloudTask[]> {
        return await this.queryService.getAllTasks();
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        return await this.taskService.getTasksByProcessInstanceId(processInstanceId);
    }

    // Audit Operations
    async getAllEvents(): Promise<CloudRuntimeEvent[]> {
        return await this.auditService.getAllEvents();
    }

    async getEventsByEntityIdAdmin(entityId: string): Promise<CloudRuntimeEvent[]> {
        return await this.auditAdminService.getEventsByEntityIdAdmin(entityId);
    }

    // Helper methods for filtering by process definition key
    filterProcessInstancesByKey(processInstances: CloudProcessInstance[], processName: string): CloudProcessInstance[] {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return processInstances.filter(
            (pi) =>
                pi.processDefinitionKey === processDefinitionKey ||
                (pi.processDefinitionId?.startsWith(`${processDefinitionKey}:`) ?? false)
        );
    }

    private async getRuntimeInstancesByProcessName(processName: string): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return this.runtimeBundleService.getProcessInstances({ processDefinitionKey });
    }

    private async getQueryInstancesByProcessName(processName: string): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return this.queryService.getProcessInstances({ processDefinitionKey });
    }

    filterEventsByProcessKey(events: CloudRuntimeEvent[], processName: string): CloudRuntimeEvent[] {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return events.filter((event) => {
            const entity = event.entity as { processDefinitionKey?: string; id?: string } | undefined;
            const entityKey = entity?.processDefinitionKey ?? event.processDefinitionKey;
            return entityKey === processDefinitionKey;
        });
    }


    filterEventsByProcessInstance(events: CloudRuntimeEvent[], processInstanceId: string, processName: string): CloudRuntimeEvent[] {
        const byKey = this.filterEventsByProcessKey(events, processName);
        if (byKey.length > 0) {
            return byKey;
        }
        return events.filter(
            (event) =>
                event.processInstanceId === processInstanceId ||
                (event.entity as { id?: string } | undefined)?.id === processInstanceId
        );
    }

    private async getEventsByProcessName(processName: string): Promise<CloudRuntimeEvent[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return this.auditService.getEvents({ processDefinitionKey });
    }

    /** Preview query may list legacy PWV rows for hradmin; assert the given instance is not visible. */
    async expectQueryDoesNotIncludeProcessInstance(processInstanceId: string, processName: string): Promise<void> {
        const instances = await this.getQueryInstancesByProcessName(processName);
        if (instances.some((pi) => pi.id === processInstanceId)) {
            throw new Error(`Expected query not to include process instance ${processInstanceId}`);
        }
    }

    // Security assertion helpers
    async expectProcessInstancesForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const filtered = shouldExist
            ? await this.getRuntimeInstancesByProcessName(processName)
            : this.filterProcessInstancesByKey(await this.getAllProcessInstances(), processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find process instances for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no process instances for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectQueryProcessInstancesForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const filtered = shouldExist
            ? await this.getQueryInstancesByProcessName(processName)
            : this.filterProcessInstancesByKey(await this.queryAllProcessInstances(), processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find process instances in query for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no process instances in query for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectEventsForKey(processName: string, shouldExist: boolean = true): Promise<CloudRuntimeEvent[]> {
        const fromApi = await this.getEventsByProcessName(processName);
        const filtered = this.filterEventsByProcessKey(fromApi, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find events for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no events for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectNoAuditEventsForProcessInstance(processInstanceId: string): Promise<void> {
        const events = await this.auditService.getEvents({ processInstanceId });
        const forInstance = events.filter(
            (event) =>
                event.processInstanceId === processInstanceId ||
                (event.entity as { id?: string } | undefined)?.id === processInstanceId
        );
        if (forInstance.length > 0) {
            throw new Error(
                `Expected no audit events for process instance ${processInstanceId}, but found ${forInstance.length}`
            );
        }
    }

    // Admin-specific operations
    async expectProcessInstancesAdminForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        let filtered: CloudProcessInstance[] = [];

        if (shouldExist) {
            await expect
                .poll(
                    async () => {
                        const allInstances =
                            await this.runtimeAdminService.getProcessInstancesWithParams({ processDefinitionKey });
                        filtered = this.filterProcessInstancesByKey(allInstances, processName);
                        return filtered.length;
                    },
                    pollOptions('querySync')
                )
                .toBeGreaterThan(0);
        } else {
            const allInstances =
                await this.runtimeAdminService.getProcessInstancesWithParams({ processDefinitionKey });
            filtered = this.filterProcessInstancesByKey(allInstances, processName);
            if (filtered.length > 0) {
                throw new Error(`Expected no admin process instances for ${processName}, but found ${filtered.length}`);
            }
        }

        return filtered;
    }

    async expectQueryProcessInstancesAdminForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        let filtered: CloudProcessInstance[] = [];

        if (shouldExist) {
            await expect
                .poll(
                    async () => {
                        const allInstances =
                            await this.queryAdminService.getProcessInstancesAdminWithParams({ processDefinitionKey });
                        filtered = this.filterProcessInstancesByKey(allInstances, processName);
                        return filtered.length;
                    },
                    pollOptions('querySync')
                )
                .toBeGreaterThan(0);
        } else {
            const allInstances =
                await this.queryAdminService.getProcessInstancesAdminWithParams({ processDefinitionKey });
            filtered = this.filterProcessInstancesByKey(allInstances, processName);
            if (filtered.length > 0) {
                throw new Error(`Expected no admin query process instances for ${processName}, but found ${filtered.length}`);
            }
        }

        return filtered;
    }

    async expectEventsAdminForKey(processInstanceId: string, processName: string, shouldExist: boolean = true): Promise<CloudRuntimeEvent[]> {
        let filtered: CloudRuntimeEvent[] = [];

        if (shouldExist) {
            await expect
                .poll(
                    async () => {
                        const allEvents = await this.getEventsByEntityIdAdmin(processInstanceId);
                        filtered = this.filterEventsByProcessInstance(allEvents, processInstanceId, processName);
                        return filtered.length;
                    },
                    pollOptions('auditEvents')
                )
                .toBeGreaterThan(0);
        } else {
            const allEvents = await this.getEventsByEntityIdAdmin(processInstanceId);
            filtered = this.filterEventsByProcessInstance(allEvents, processInstanceId, processName);
            if (filtered.length > 0) {
                throw new Error(`Expected no admin events for ${processName}, but found ${filtered.length}`);
            }
        }

        return filtered;
    }
}
