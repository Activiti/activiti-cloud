/*
 * Multi-runtime bundle assertions — async status polling via Playwright expect.
 */

import { expect } from '@playwright/test';
import { pollOptions, timeouts } from '../config/runtime/timeouts';
import { CloudProcessInstance, ProcessInstanceStatus } from '../models/runtime-bundle.models';
import { QueryService } from '../services/query.service';
import { getQueryProcessInstanceWhenSynced } from './query-sync';

export async function waitForProcessInstanceStatus(
    queryService: QueryService,
    processInstanceId: string,
    expectedStatus: ProcessInstanceStatus,
    timeoutMs: number = timeouts.poll.signalProcess
): Promise<CloudProcessInstance> {
    let lastInstance: CloudProcessInstance | undefined;

    await expect
        .poll(
            async () => {
                lastInstance = await getQueryProcessInstanceWhenSynced(queryService, processInstanceId);
                return lastInstance?.status;
            },
            { ...pollOptions('processStatus', timeouts.intervals.fast), timeout: timeoutMs }
        )
        .toBe(expectedStatus);

    return lastInstance!;
}
