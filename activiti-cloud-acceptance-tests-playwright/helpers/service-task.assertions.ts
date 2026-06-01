/*
 * Service task and integration context assertions.
 */

import { expect } from '@playwright/test';
import { CloudRuntimeEvent } from '../models/audit.models';
import { CloudProcessInstance, ProcessInstanceStatus } from '../models/runtime-bundle.models';
import { CloudIntegrationContext, CloudServiceTask } from '../models/service-task.models';
import { AuditService } from '../services/audit.service';
import { QueryAdminService } from '../services/query-admin.service';
import { QueryService } from '../services/query.service';
import { expectPoll } from './expect-poll';
import { getQueryProcessInstanceWhenSynced } from './query-sync';

const INTEGRATION_EVENT_TYPES = {
    requested: 'INTEGRATION_REQUESTED',
    resultReceived: 'INTEGRATION_RESULT_RECEIVED',
    errorReceived: 'INTEGRATION_ERROR_RECEIVED',
} as const;

function integrationEntity(event: CloudRuntimeEvent): CloudIntegrationContext {
    return (event.entity ?? {}) as CloudIntegrationContext;
}

function eventMatchesProcess(event: CloudRuntimeEvent, processInstance: CloudProcessInstance): boolean {
    const entity = integrationEntity(event);
    return (
        event.processInstanceId === processInstance.id &&
        event.processDefinitionKey === processInstance.processDefinitionKey &&
        entity.processInstanceId === processInstance.id &&
        entity.processDefinitionId === processInstance.processDefinitionId
    );
}

export async function expectProcessCompletedInQuery(
    queryService: QueryService,
    processInstanceId: string
): Promise<void> {
    await expectPoll(async () => {
        const instance = await getQueryProcessInstanceWhenSynced(queryService, processInstanceId);
        return instance?.status;
    }, 'querySync').toBe(ProcessInstanceStatus.COMPLETED);
}

export async function expectProcessCancelledInQuery(
    queryService: QueryService,
    processInstanceId: string
): Promise<void> {
    await expectPoll(async () => {
        const instance = await getQueryProcessInstanceWhenSynced(queryService, processInstanceId);
        return instance?.status;
    }, 'querySync').toBe(ProcessInstanceStatus.CANCELLED);
}

export async function waitForServiceTasks(
    queryAdminService: QueryAdminService,
    processInstanceId: string,
    minCount = 1
): Promise<CloudServiceTask[]> {
    let tasks: CloudServiceTask[] = [];
    await expectPoll(async () => {
        tasks = await queryAdminService.getServiceTasksForProcessInstance(processInstanceId);
        return tasks.length;
    }, 'querySync').toBeGreaterThanOrEqual(minCount);
    return tasks;
}

export async function expectServiceTasksForProcessInstance(
    queryAdminService: QueryAdminService,
    processInstanceId: string
): Promise<CloudServiceTask[]> {
    const tasks = await waitForServiceTasks(queryAdminService, processInstanceId);
    expect(tasks.length).toBeGreaterThan(0);
    tasks.forEach((task) => expect(task.activityType).toBe('serviceTask'));
    return tasks;
}

export async function expectServiceTasksByStatus(
    queryAdminService: QueryAdminService,
    processInstanceId: string,
    status: string
): Promise<CloudServiceTask[]> {
    let tasks: CloudServiceTask[] = [];
    await expectPoll(async () => {
        tasks = await queryAdminService.getServiceTasksByStatus(processInstanceId, status);
        return tasks.length;
    }, 'querySync').toBeGreaterThan(0);

    tasks.forEach((task) => {
        expect(task.activityType).toBe('serviceTask');
        expect(task.status).toBe(status);
    });
    return tasks;
}

export async function expectIntegrationContextEvents(
    auditService: AuditService,
    processInstance: CloudProcessInstance
): Promise<void> {
    await expectPoll(async () => {
        const events = await auditService.getEventsByProcessInstanceId(processInstance.id);
        const integrationEvents = events.filter((event) =>
            String(event.eventType).includes('INTEGRATION')
        );
        const types = new Set(integrationEvents.map((event) => event.eventType));
        return (
            types.has(INTEGRATION_EVENT_TYPES.requested) &&
            types.has(INTEGRATION_EVENT_TYPES.resultReceived) &&
            integrationEvents.every((event) => eventMatchesProcess(event, processInstance))
        );
    }, 'auditEvents').toBe(true);
}

export async function expectIntegrationErrorEvents(
    auditService: AuditService,
    processInstance: CloudProcessInstance
): Promise<void> {
    await expectPoll(async () => {
        const events = await auditService.getEventsByProcessInstanceId(processInstance.id);
        const integrationEvents = events.filter((event) =>
            String(event.eventType).includes('INTEGRATION')
        );
        const types = new Set(integrationEvents.map((event) => event.eventType));
        return (
            types.has(INTEGRATION_EVENT_TYPES.requested) &&
            types.has(INTEGRATION_EVENT_TYPES.errorReceived) &&
            integrationEvents.every((event) => eventMatchesProcess(event, processInstance))
        );
    }, 'auditEvents').toBe(true);
}

export async function expectAllIntegrationContextEvents(
    auditService: AuditService,
    processInstance: CloudProcessInstance
): Promise<void> {
    await expectPoll(async () => {
        const events = await auditService.getEventsByProcessInstanceId(processInstance.id);
        const integrationEvents = events.filter((event) =>
            String(event.eventType).includes('INTEGRATION')
        );
        const types = integrationEvents.map((event) => event.eventType);
        const requestedCount = types.filter((t) => t === INTEGRATION_EVENT_TYPES.requested).length;
        const resultCount = types.filter((t) => t === INTEGRATION_EVENT_TYPES.resultReceived).length;
        const errorCount = types.filter((t) => t === INTEGRATION_EVENT_TYPES.errorReceived).length;
        return (
            requestedCount >= 2 &&
            resultCount >= 1 &&
            errorCount >= 1 &&
            integrationEvents.every((event) => eventMatchesProcess(event, processInstance))
        );
    }, 'auditEvents').toBe(true);
}

export async function expectIntegrationContextForServiceTask(
    queryAdminService: QueryAdminService,
    serviceTaskId: string
): Promise<CloudIntegrationContext> {
    let context!: CloudIntegrationContext;
    await expectPoll(async () => {
        context = await queryAdminService.getIntegrationContext(serviceTaskId);
        return context?.clientType === 'ServiceTask' && context?.status === 'INTEGRATION_RESULT_RECEIVED';
    }, 'querySync').toBe(true);
    return context;
}

export async function expectTwoIntegrationContextsForServiceTask(
    queryAdminService: QueryAdminService,
    serviceTaskId: string
): Promise<CloudIntegrationContext[]> {
    let contexts: CloudIntegrationContext[] = [];
    await expectPoll(async () => {
        contexts = await queryAdminService.getAllIntegrationContexts(serviceTaskId);
        return (
            contexts.length === 2 &&
            contexts.every(
                (ctx) => ctx.clientType === 'ServiceTask' && ctx.status === 'INTEGRATION_RESULT_RECEIVED'
            )
        );
    }, 'querySync').toBe(true);
    return contexts;
}

export async function expectServiceTaskIntegrationCounter(
    queryAdminService: QueryAdminService,
    processInstanceId: string,
    expectedCount: number
): Promise<CloudServiceTask> {
    let serviceTask!: CloudServiceTask;
    await expectPoll(async () => {
        const tasks = await queryAdminService.getServiceTasksForProcessInstance(processInstanceId);
        if (tasks.length !== 1) {
            return false;
        }
        serviceTask = tasks[0];
        return serviceTask.integrationContextCounter === expectedCount;
    }, 'querySync').toBe(true);
    return serviceTask;
}

export async function expectServiceTasksInGlobalQuery(
    queryAdminService: QueryAdminService,
    processInstanceId: string,
    processDefinitionKey: string,
    status: string,
    processDefinitionId?: string
): Promise<void> {
    await expectPoll(async () => {
        const tasks = await queryAdminService.getServiceTasksByQuery({
            processDefinitionKey,
            status,
        });
        const forInstance = tasks.filter((task) => task.processInstanceId === processInstanceId);
        if (forInstance.length === 0) {
            return false;
        }
        return forInstance.every(
            (task) =>
                task.activityType === 'serviceTask' &&
                task.status === status &&
                task.processDefinitionKey === processDefinitionKey &&
                (!processDefinitionId || task.processDefinitionId === processDefinitionId)
        );
    }, 'querySync').toBe(true);
}

export async function findIntegrationErrorEvent(
    auditService: AuditService,
    processInstanceId: string
): Promise<CloudRuntimeEvent | undefined> {
    const events = await auditService.getEventsByProcessInstanceId(processInstanceId);
    return events.find((event) => event.eventType === INTEGRATION_EVENT_TYPES.errorReceived);
}
