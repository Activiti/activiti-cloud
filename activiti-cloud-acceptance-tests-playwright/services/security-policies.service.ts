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

import { APIRequestContext } from '@playwright/test';
import { RuntimeBundleService } from './runtime-bundle.service';
import { TaskService } from './task.service';
import { QueryService } from './query.service';
import { AuditService, AuditAdminService } from './audit.service';
import { RuntimeAdminService, QueryAdminService } from './admin.service';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { CloudTask } from '../models/task.models';
import { CloudRuntimeEvent } from '../models/audit.models';
import { ProcessDefinitionRegistry } from '../models/process-definition-registry';

export class SecurityPoliciesService {
    private readonly runtimeBundleService: RuntimeBundleService;
    private readonly taskService: TaskService;
    private readonly queryService: QueryService;
    private readonly auditService: AuditService;
    private readonly auditAdminService: AuditAdminService;
    private readonly runtimeAdminService: RuntimeAdminService;
    private readonly queryAdminService: QueryAdminService;

    constructor(private readonly context: APIRequestContext) {
        this.runtimeBundleService = new RuntimeBundleService(context);
        this.taskService = new TaskService(context);
        this.queryService = new QueryService(context);
        this.auditService = new AuditService(context);
        this.auditAdminService = new AuditAdminService(context);
        this.runtimeAdminService = new RuntimeAdminService(context);
        this.queryAdminService = new QueryAdminService(context);
    }

    // Process Instance Operations
    async startProcess(processName: string): Promise<CloudProcessInstance> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return await this.runtimeBundleService.startProcess({ processDefinitionKey });
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return await this.runtimeBundleService.getProcessInstances();
    }

    async getProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        return await this.runtimeAdminService.getProcessInstances();
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
        return processInstances.filter(pi => pi.processDefinitionKey === processDefinitionKey);
    }

    filterEventsByProcessKey(events: CloudRuntimeEvent[], processName: string): CloudRuntimeEvent[] {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return events.filter(event => event.processDefinitionKey === processDefinitionKey);
    }

    // Security assertion helpers
    async expectProcessInstancesForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const allInstances = await this.getAllProcessInstances();
        const filtered = this.filterProcessInstancesByKey(allInstances, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find process instances for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no process instances for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectQueryProcessInstancesForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const allInstances = await this.queryAllProcessInstances();
        const filtered = this.filterProcessInstancesByKey(allInstances, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find process instances in query for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no process instances in query for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectEventsForKey(processName: string, shouldExist: boolean = true): Promise<CloudRuntimeEvent[]> {
        const allEvents = await this.getAllEvents();
        const filtered = this.filterEventsByProcessKey(allEvents, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find events for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no events for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    // Admin-specific operations
    async expectProcessInstancesAdminForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const allInstances = await this.getProcessInstancesAdmin();
        const filtered = this.filterProcessInstancesByKey(allInstances, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find admin process instances for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no admin process instances for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectQueryProcessInstancesAdminForKey(processName: string, shouldExist: boolean = true): Promise<CloudProcessInstance[]> {
        const allInstances = await this.queryAllProcessInstancesAdmin();
        const filtered = this.filterProcessInstancesByKey(allInstances, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find admin query process instances for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no admin query process instances for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }

    async expectEventsAdminForKey(processInstanceId: string, processName: string, shouldExist: boolean = true): Promise<CloudRuntimeEvent[]> {
        const allEvents = await this.getEventsByEntityIdAdmin(processInstanceId);
        const filtered = this.filterEventsByProcessKey(allEvents, processName);

        if (shouldExist && filtered.length === 0) {
            throw new Error(`Expected to find admin events for ${processName}, but found none`);
        } else if (!shouldExist && filtered.length > 0) {
            throw new Error(`Expected no admin events for ${processName}, but found ${filtered.length}`);
        }

        return filtered;
    }
}
