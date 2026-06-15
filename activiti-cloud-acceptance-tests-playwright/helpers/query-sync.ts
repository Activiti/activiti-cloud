/*
 * Query sync helpers — Kafka and other brokers can lag before query exposes a process instance.
 */

import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { QueryService } from '../services/query.service';
import { QueryAdminService } from '../services/query-admin.service';

export function isQueryProcessInstanceNotFoundError(error: unknown): boolean {
    const message = error instanceof Error ? error.message : String(error);
    return message.includes('Unable to find process instance');
}

async function returnUndefinedWhenNotFound<T>(loader: () => Promise<T>): Promise<T | undefined> {
    try {
        return await loader();
    } catch (error) {
        if (isQueryProcessInstanceNotFoundError(error)) {
            return undefined;
        }
        throw error;
    }
}

export function getQueryProcessInstanceWhenSynced(
    queryService: QueryService,
    processInstanceId: string
): Promise<CloudProcessInstance | undefined> {
    return returnUndefinedWhenNotFound(() => queryService.getProcessInstance(processInstanceId));
}

export function getQueryProcessInstanceAdminWhenSynced(
    queryAdminService: QueryAdminService,
    processInstanceId: string
): Promise<CloudProcessInstance | undefined> {
    return returnUndefinedWhenNotFound(() =>
        queryAdminService.getProcessInstanceAdmin(processInstanceId)
    );
}

export async function loadOrUndefined<T>(loader: () => Promise<T>): Promise<T | undefined> {
    try {
        return await loader();
    } catch {
        return undefined;
    }
}
