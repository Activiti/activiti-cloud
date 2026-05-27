/*
 * Security policy assertions — Playwright expect/poll only; no HTTP in this module.
 */

import { expect } from '@playwright/test';
import { pollOptions } from '../config/runtime/timeouts';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { CloudRuntimeEvent } from '../models/audit.models';
import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { SecurityPoliciesService } from '../services/security-policies.service';

export async function expectProcessInstancesForKey(
    service: SecurityPoliciesService,
    processName: string,
    shouldExist: boolean = true
): Promise<CloudProcessInstance[]> {
    const filtered = shouldExist
        ? await service.getRuntimeInstancesByProcessName(processName)
        : service.filterProcessInstancesByKey(await service.getAllProcessInstances(), processName);

    if (shouldExist && filtered.length === 0) {
        throw new Error(`Expected to find process instances for ${processName}, but found none`);
    }
    if (!shouldExist && filtered.length > 0) {
        throw new Error(`Expected no process instances for ${processName}, but found ${filtered.length}`);
    }

    return filtered;
}

export async function expectQueryProcessInstancesForKey(
    service: SecurityPoliciesService,
    processName: string,
    shouldExist: boolean = true
): Promise<CloudProcessInstance[]> {
    const filtered = shouldExist
        ? await service.getQueryInstancesByProcessName(processName)
        : service.filterProcessInstancesByKey(await service.queryAllProcessInstances(), processName);

    if (shouldExist && filtered.length === 0) {
        throw new Error(`Expected to find process instances in query for ${processName}, but found none`);
    }
    if (!shouldExist && filtered.length > 0) {
        throw new Error(`Expected no process instances in query for ${processName}, but found ${filtered.length}`);
    }

    return filtered;
}

export async function expectEventsForKey(
    service: SecurityPoliciesService,
    processName: string,
    shouldExist: boolean = true
): Promise<CloudRuntimeEvent[]> {
    const fromApi = await service.getEventsByProcessName(processName);
    const filtered = service.filterEventsByProcessKey(fromApi, processName);

    if (shouldExist && filtered.length === 0) {
        throw new Error(`Expected to find events for ${processName}, but found none`);
    }
    if (!shouldExist && filtered.length > 0) {
        throw new Error(`Expected no events for ${processName}, but found ${filtered.length}`);
    }

    return filtered;
}

export async function expectQueryDoesNotIncludeProcessInstance(
    service: SecurityPoliciesService,
    processInstanceId: string,
    processName: string
): Promise<void> {
    const instances = await service.getQueryInstancesByProcessName(processName);
    if (instances.some((pi) => pi.id === processInstanceId)) {
        throw new Error(`Expected query not to include process instance ${processInstanceId}`);
    }
}

export async function expectNoAuditEventsForProcessInstance(
    service: SecurityPoliciesService,
    processInstanceId: string
): Promise<void> {
    const events = await service.getAuditEventsForProcessInstance(processInstanceId);
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

export async function expectProcessInstancesAdminForKey(
    service: SecurityPoliciesService,
    processName: string,
    shouldExist: boolean = true
): Promise<CloudProcessInstance[]> {
    const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
    let filtered: CloudProcessInstance[] = [];

    if (shouldExist) {
        await expect
            .poll(
                async () => {
                    const allInstances = await service.getRuntimeAdminProcessInstances({ processDefinitionKey });
                    filtered = service.filterProcessInstancesByKey(allInstances, processName);
                    return filtered.length;
                },
                pollOptions('querySync')
            )
            .toBeGreaterThan(0);
    } else {
        const allInstances = await service.getRuntimeAdminProcessInstances({ processDefinitionKey });
        filtered = service.filterProcessInstancesByKey(allInstances, processName);
        if (filtered.length > 0) {
            throw new Error(`Expected no admin process instances for ${processName}, but found ${filtered.length}`);
        }
    }

    return filtered;
}

export async function expectQueryProcessInstancesAdminForKey(
    service: SecurityPoliciesService,
    processName: string,
    shouldExist: boolean = true
): Promise<CloudProcessInstance[]> {
    const processDefinitionKey = ProcessDefinitionRegistry.getProcessDefinitionKey(processName);
    let filtered: CloudProcessInstance[] = [];

    if (shouldExist) {
        await expect
            .poll(
                async () => {
                    const allInstances = await service.getQueryAdminProcessInstances({ processDefinitionKey });
                    filtered = service.filterProcessInstancesByKey(allInstances, processName);
                    return filtered.length;
                },
                pollOptions('querySync')
            )
            .toBeGreaterThan(0);
    } else {
        const allInstances = await service.getQueryAdminProcessInstances({ processDefinitionKey });
        filtered = service.filterProcessInstancesByKey(allInstances, processName);
        if (filtered.length > 0) {
            throw new Error(`Expected no admin query process instances for ${processName}, but found ${filtered.length}`);
        }
    }

    return filtered;
}

export async function expectEventsAdminForKey(
    service: SecurityPoliciesService,
    processInstanceId: string,
    processName: string,
    shouldExist: boolean = true
): Promise<CloudRuntimeEvent[]> {
    let filtered: CloudRuntimeEvent[] = [];

    if (shouldExist) {
        await expect
            .poll(
                async () => {
                    const allEvents = await service.getEventsByEntityIdAdmin(processInstanceId);
                    filtered = service.filterEventsByProcessInstance(allEvents, processInstanceId, processName);
                    return filtered.length;
                },
                pollOptions('auditEvents')
            )
            .toBeGreaterThan(0);
    } else {
        const allEvents = await service.getEventsByEntityIdAdmin(processInstanceId);
        filtered = service.filterEventsByProcessInstance(allEvents, processInstanceId, processName);
        if (filtered.length > 0) {
            throw new Error(`Expected no admin events for ${processName}, but found ${filtered.length}`);
        }
    }

    return filtered;
}
