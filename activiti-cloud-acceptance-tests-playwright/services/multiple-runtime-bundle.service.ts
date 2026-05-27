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

import { expect } from '@playwright/test';
import { pollOptions, timeouts } from '../config/runtime/timeouts';
import { DirtyContextRegistry } from '../helpers/dirty-context';
import { TestScope } from '../helpers/test-isolation';
import { RuntimeBundleService } from './runtime-bundle.service';
import { QueryService } from './query.service';
import {
    CloudProcessInstance,
    ProcessInstanceStatus
} from '../models/runtime-bundle.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../context.models';

/**
 * Serenity uses /rb-other-app for a second runtime. Preview installs often expose only /rb;
 * both processes still complete on one engine (signals via RabbitMQ). Set SECONDARY_RUNTIME_BASE_PATH=/rb-other-app when deployed.
 */
const SECONDARY_RUNTIME_BASE_PATH = process.env.SECONDARY_RUNTIME_BASE_PATH?.trim() || '/rb';

export class MultipleRuntimeBundleService extends BaseService {
    private readonly primaryRuntimeService: RuntimeBundleService;
    private readonly secondaryRuntimeService: RuntimeBundleService;
    private readonly queryService: QueryService;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.primaryRuntimeService = new RuntimeBundleService(context);
        this.secondaryRuntimeService = new RuntimeBundleService(context, SECONDARY_RUNTIME_BASE_PATH);
        this.queryService = new QueryService(context);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope): void {
        super.attachIsolation(dirtyRegistry, testScope);
        this.primaryRuntimeService.attachIsolation(dirtyRegistry, testScope, '/rb/v1');
        this.secondaryRuntimeService.attachIsolation(
            dirtyRegistry,
            testScope,
            `${SECONDARY_RUNTIME_BASE_PATH.replace(/\/$/, '')}/v1`
        );
    }

    async startProcessOnPrimary(processDefinitionKey: string): Promise<CloudProcessInstance> {
        return this.primaryRuntimeService.startProcess({ processDefinitionKey });
    }

    async startProcessOnSecondary(processDefinitionKey: string): Promise<CloudProcessInstance> {
        return this.secondaryRuntimeService.startProcess({ processDefinitionKey });
    }

    async getProcessInstanceFromPrimary(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.queryService.getProcessInstance(processInstanceId);
    }

    async getProcessInstanceFromSecondary(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.queryService.getProcessInstance(processInstanceId);
    }

    async waitForProcessInstanceStatusOnPrimary(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus,
        timeoutMs: number = timeouts.poll.signalProcess
    ): Promise<CloudProcessInstance> {
        return this.waitForProcessInstanceStatus(
            processInstanceId,
            expectedStatus,
            timeoutMs,
            this.primaryRuntimeService
        );
    }

    async waitForProcessInstanceStatusOnSecondary(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus,
        timeoutMs: number = timeouts.poll.signalProcess
    ): Promise<CloudProcessInstance> {
        return this.waitForProcessInstanceStatus(
            processInstanceId,
            expectedStatus,
            timeoutMs,
            this.secondaryRuntimeService
        );
    }

    private async waitForProcessInstanceStatus(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus,
        timeoutMs: number,
        _runtime: RuntimeBundleService
    ): Promise<CloudProcessInstance> {
        let lastInstance: CloudProcessInstance | undefined;

        await expect
            .poll(
                async () => {
                    lastInstance = await this.queryService.getProcessInstance(processInstanceId);
                    return lastInstance?.status;
                },
                { ...pollOptions('processStatus', timeouts.intervals.fast), timeout: timeoutMs }
            )
            .toBe(expectedStatus);

        return lastInstance!;
    }
}
