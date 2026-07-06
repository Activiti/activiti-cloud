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
import { QueryService } from './query/query.service';
import { AuditService } from './audit/audit.service';
import { RuntimeAdminService } from './runtime-admin.service';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { CloudTask } from '../models/task.models';
import { CloudRuntimeEvent } from '../models/audit.models';
import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { BaseService, RequestResponse } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';
import { DirtyContextRegistry } from '../helpers/dirty-context';
import { TestScope } from '../helpers/test-isolation';

interface ErrorEntry {
    code?: number;
    message?: string;
}

export class SecurityPoliciesService extends BaseService {
    private readonly runtimeBundleService: RuntimeBundleService;
    private readonly taskService: TaskService;
    private readonly queryService: QueryService;
    private readonly auditService: AuditService;
    private readonly runtimeAdminService: RuntimeAdminService;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.runtimeBundleService = new RuntimeBundleService(context);
        this.taskService = new TaskService(context);
        this.queryService = new QueryService(context);
        this.auditService = new AuditService(context);
        this.runtimeAdminService = new RuntimeAdminService(context);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope): void {
        super.attachIsolation(dirtyRegistry, testScope);
        this.runtimeBundleService.attachIsolation(dirtyRegistry, testScope);
        this.taskService.attachIsolation(dirtyRegistry, testScope);
    }

    async startProcess(processName: string): Promise<CloudProcessInstance> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        const startPath =
            this.context.username === 'hradmin' || this.context.username === 'processadmin'
                ? '/rb/admin/v1/process-instances'
                : '/rb/v1/process-instances';

        const response = await this.post(startPath, {
            data: {
                payloadType: 'StartProcessPayload',
                processDefinitionKey,
            },
        });

        this.throwOnClientError(response, processDefinitionKey);

        const processInstance = this.unwrapEntity<CloudProcessInstance>(response);
        if (processInstance.id) {
            this.dirtyRegistry?.register(this.context, `${startPath}/${processInstance.id}`);
        }
        return processInstance;
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return this.runtimeBundleService.getProcessInstances();
    }

    async getRuntimeProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.runtimeBundleService.getProcessInstance(processInstanceId);
    }

    async getRuntimeInstancesByProcessName(processName: string): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return this.runtimeBundleService.getProcessInstances({ processDefinitionKey });
    }

    async getQueryInstancesByProcessName(processName: string): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return this.queryService.processInstances.getProcessInstances({ processDefinitionKey });
    }

    async waitForQueryInstancesByProcessName(processName: string): Promise<CloudProcessInstance[]> {
        return SecurityPoliciesService.waitFor(
            () => this.getQueryInstancesByProcessName(processName),
            (instances) => instances.length > 0,
            'querySync',
            `query process instances for ${processName}`
        );
    }

    async getEventsByProcessName(processName: string): Promise<CloudRuntimeEvent[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return this.auditService.events.getEvents({ processDefinitionKey });
    }

    async getAuditEventsForProcessInstance(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        return this.auditService.events.getEvents({ processInstanceId });
    }

    async getRuntimeAdminProcessInstances(params: {
        processDefinitionKey: string;
    }): Promise<CloudProcessInstance[]> {
        return this.runtimeAdminService.getProcessInstancesWithParams(params);
    }

    async getQueryAdminProcessInstances(params: {
        processDefinitionKey: string;
    }): Promise<CloudProcessInstance[]> {
        return this.queryService.adminProcessInstances.getProcessInstances(params);
    }

    async queryAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return this.queryService.processInstances.getAllProcessInstances();
    }

    async getAllTasks(): Promise<CloudTask[]> {
        return this.taskService.getAllTasks();
    }

    async queryAllTasks(): Promise<CloudTask[]> {
        return this.queryService.tasks.getAllTasks();
    }

    async getEventsByEntityIdAdmin(entityId: string): Promise<CloudRuntimeEvent[]> {
        return this.auditService.getEventsByEntityIdAdmin(entityId);
    }

    async getFilteredAllRuntimeInstancesByName(processName: string): Promise<CloudProcessInstance[]> {
        return this.filterProcessInstancesByKey(await this.getAllProcessInstances(), processName);
    }

    async getFilteredAllQueryInstancesByName(processName: string): Promise<CloudProcessInstance[]> {
        return this.filterProcessInstancesByKey(await this.queryAllProcessInstances(), processName);
    }

    async getFilteredEventsByName(processName: string): Promise<CloudRuntimeEvent[]> {
        return this.filterEventsByProcessKey(await this.getEventsByProcessName(processName), processName);
    }

    async waitForFilteredEventsByName(processName: string): Promise<CloudRuntimeEvent[]> {
        return SecurityPoliciesService.waitFor(
            () => this.getFilteredEventsByName(processName),
            (events) => events.length > 0,
            'auditEvents',
            `audit events for ${processName}`
        );
    }

    async getFilteredRuntimeAdminInstancesByName(processName: string): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        const all = await this.getRuntimeAdminProcessInstances({ processDefinitionKey });
        return this.filterProcessInstancesByKey(all, processName);
    }

    async getFilteredQueryAdminInstancesByName(processName: string): Promise<CloudProcessInstance[]> {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        const all = await this.getQueryAdminProcessInstances({ processDefinitionKey });
        return this.filterProcessInstancesByKey(all, processName);
    }

    async waitForFilteredRuntimeAdminInstancesByName(processName: string): Promise<CloudProcessInstance[]> {
        return SecurityPoliciesService.waitFor(
            () => this.getFilteredRuntimeAdminInstancesByName(processName),
            (instances) => instances.length > 0,
            'querySync',
            `runtime admin process instances for ${processName}`
        );
    }

    async waitForFilteredQueryAdminInstancesByName(processName: string): Promise<CloudProcessInstance[]> {
        return SecurityPoliciesService.waitFor(
            () => this.getFilteredQueryAdminInstancesByName(processName),
            (instances) => instances.length > 0,
            'querySync',
            `query admin process instances for ${processName}`
        );
    }

    async getFilteredAdminEventsForProcessInstance(
        processInstanceId: string,
        processName: string
    ): Promise<CloudRuntimeEvent[]> {
        const all = await this.getEventsByEntityIdAdmin(processInstanceId);
        return this.filterEventsByProcessInstance(all, processInstanceId, processName);
    }

    async waitForFilteredAdminEventsForProcessInstance(
        processInstanceId: string,
        processName: string
    ): Promise<CloudRuntimeEvent[]> {
        return SecurityPoliciesService.waitFor(
            () => this.getFilteredAdminEventsForProcessInstance(processInstanceId, processName),
            (events) => events.length > 0,
            'auditEvents',
            `admin events for process ${processInstanceId} (${processName})`
        );
    }

    async getAuditEventsByProcessInstanceFiltered(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        const events = await this.getAuditEventsForProcessInstance(processInstanceId);
        return events.filter(
            (event) =>
                event.processInstanceId === processInstanceId ||
                (event.entity as { id?: string } | undefined)?.id === processInstanceId
        );
    }

    filterProcessInstancesByKey(processInstances: CloudProcessInstance[], processName: string): CloudProcessInstance[] {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return processInstances.filter(
            (pi) =>
                pi.processDefinitionKey === processDefinitionKey ||
                (pi.processDefinitionId?.startsWith(`${processDefinitionKey}:`) ?? false)
        );
    }

    filterEventsByProcessKey(events: CloudRuntimeEvent[], processName: string): CloudRuntimeEvent[] {
        const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
        return events.filter((event) => {
            const entity = event.entity as { processDefinitionKey?: string; id?: string } | undefined;
            const entityKey = entity?.processDefinitionKey ?? event.processDefinitionKey;
            return entityKey === processDefinitionKey;
        });
    }

    filterEventsByProcessInstance(
        events: CloudRuntimeEvent[],
        processInstanceId: string,
        processName: string
    ): CloudRuntimeEvent[] {
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

    private throwOnClientError(response: RequestResponse, processDefinitionKey: string): void {
        if (response.httpStatus && response.httpStatus >= 400 && response.httpStatus < 500) {
            const entry = response.entry as ErrorEntry | undefined;
            const errorMessage =
                entry?.message ||
                (typeof response.message === 'string' ? response.message : undefined) ||
                response.body ||
                `Unable to find process definition for the given id:'${processDefinitionKey}'`;
            throw new Error(String(errorMessage));
        }

        const entry = response.entry as ErrorEntry | undefined;
        if (entry?.code && entry?.message) {
            throw new Error(entry.message);
        }
    }
}
