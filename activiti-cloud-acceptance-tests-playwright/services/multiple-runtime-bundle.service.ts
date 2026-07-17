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

import { timeouts } from '../config/runtime/timeouts';
import { DirtyContextRegistry } from '../helpers/dirty-context';
import { TestScope } from '../helpers/test-isolation';
import { RuntimeBundleService } from './runtime-bundle/runtime-bundle.service';
import { QueryService } from './query/query.service';
import { CloudProcessInstance, ProcessInstanceStatus } from '../models/runtime-bundle.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

const SECONDARY_RUNTIME_BASE_PATH = process.env.SECONDARY_RUNTIME_BASE_PATH?.trim() || '/rb';

export class MultipleRuntimeBundleService extends BaseService {
    readonly primary: RuntimeBundleService;
    readonly secondary: RuntimeBundleService;
    readonly query: QueryService;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.primary = new RuntimeBundleService(context);
        this.secondary = new RuntimeBundleService(context, SECONDARY_RUNTIME_BASE_PATH);
        this.query = new QueryService(context);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope): void {
        super.attachIsolation(dirtyRegistry, testScope);
        this.primary.attachIsolation(dirtyRegistry, testScope, '/rb/v1');
        this.secondary.attachIsolation(
            dirtyRegistry,
            testScope,
            `${SECONDARY_RUNTIME_BASE_PATH.replace(/\/$/, '')}/v1`
        );
    }

    async waitForProcessInstanceStatusOnPrimary(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus
    ): Promise<CloudProcessInstance> {
        const instance = await MultipleRuntimeBundleService.waitFor(
            () => this.query.getProcessInstanceWhenSynced(processInstanceId),
            (value) => value?.status === expectedStatus,
            'signalProcess',
            `process ${processInstanceId} to reach status ${expectedStatus} on primary runtime`,
            timeouts.intervals.fast
        );
        return instance!;
    }

    async waitForProcessInstanceStatusOnSecondary(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus
    ): Promise<CloudProcessInstance> {
        const instance = await MultipleRuntimeBundleService.waitFor(
            () => this.query.getProcessInstanceWhenSynced(processInstanceId),
            (value) => value?.status === expectedStatus,
            'signalProcess',
            `process ${processInstanceId} to reach status ${expectedStatus} on secondary runtime`,
            timeouts.intervals.fast
        );
        return instance!;
    }

    get signalPollTimeoutMs(): number {
        return timeouts.poll.signalProcess;
    }
}
