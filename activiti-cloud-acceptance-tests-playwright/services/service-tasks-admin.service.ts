/*
 * Runtime admin — replay failed service task executions.
 */

import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export class ServiceTasksAdminService extends BaseService {
    private readonly basePath = '/rb/admin/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async replayServiceTask(executionId: string, flowNodeId: string): Promise<void> {
        await this.post(`${this.basePath}/executions/${executionId}/replay/service-task`, {
            data: { flowNodeId },
        });
    }
}
