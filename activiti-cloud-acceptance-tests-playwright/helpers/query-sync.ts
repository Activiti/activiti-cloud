/*
 * Query sync helpers — Kafka and other brokers can lag before query exposes a process instance.
 */

import { CloudProcessInstance } from '../models/runtime-bundle.models';
import { QueryService } from '../services/query.service';

export function isQueryProcessInstanceNotFoundError(error: unknown): boolean {
    const message = error instanceof Error ? error.message : String(error);
    return message.includes('Unable to find process instance');
}

export async function getQueryProcessInstanceWhenSynced(
    queryService: QueryService,
    processInstanceId: string
): Promise<CloudProcessInstance | undefined> {
    try {
        return await queryService.getProcessInstance(processInstanceId);
    } catch (error) {
        if (isQueryProcessInstanceNotFoundError(error)) {
            return undefined;
        }
        throw error;
    }
}
