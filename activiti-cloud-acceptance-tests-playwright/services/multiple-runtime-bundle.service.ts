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
import {
    CloudProcessInstance,
    StartProcessPayload,
    ProcessInstanceStatus
} from '../models/runtime-bundle.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../context.models';

export class MultipleRuntimeBundleService extends BaseService {
    private readonly primaryRuntimeService: RuntimeBundleService;
    private readonly secondaryRuntimeService: RuntimeBundleService;

    constructor(context: CustomAPIRequest) {
        super(context);
        // Primary runtime bundle
        this.primaryRuntimeService = new RuntimeBundleService(context);
        // Secondary runtime bundle - in a real multi-runtime setup, this would use a different base path
        // For now, we'll use the same service but with different process keys to simulate different runtimes
        this.secondaryRuntimeService = new RuntimeBundleService(context);
    }

    async startProcessOnPrimary(processDefinitionKey: string): Promise<CloudProcessInstance> {
        const payload: StartProcessPayload = {
            processDefinitionKey: processDefinitionKey
        };

        return await this.primaryRuntimeService.startProcess(payload);
    }

    async startProcessOnSecondary(processDefinitionKey: string): Promise<CloudProcessInstance> {
        const payload: StartProcessPayload = {
            processDefinitionKey: processDefinitionKey
        };

        return await this.secondaryRuntimeService.startProcess(payload);
    }

    async getProcessInstanceFromPrimary(processInstanceId: string): Promise<CloudProcessInstance> {
        return await this.primaryRuntimeService.getProcessInstance(processInstanceId);
    }

    async getProcessInstanceFromSecondary(processInstanceId: string): Promise<CloudProcessInstance> {
        return await this.secondaryRuntimeService.getProcessInstance(processInstanceId);
    }

    async waitForProcessInstanceStatusOnPrimary(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus,
        timeoutMs: number = 30000
    ): Promise<CloudProcessInstance> {
        const startTime = Date.now();

        while (Date.now() - startTime < timeoutMs) {
            try {
                const processInstance = await this.getProcessInstanceFromPrimary(processInstanceId);

                if (processInstance.status === expectedStatus) {
                    return processInstance;
                }

                // Wait a bit before next check
                await new Promise(resolve => setTimeout(resolve, 1000));
            } catch (error) {
                // If process instance not found yet, continue waiting
                if (Date.now() - startTime >= timeoutMs) {
                    throw error;
                }
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        }

        throw new Error(`Process instance ${processInstanceId} did not reach status ${expectedStatus} within ${timeoutMs}ms`);
    }

    async waitForProcessInstanceStatusOnSecondary(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus,
        timeoutMs: number = 30000
    ): Promise<CloudProcessInstance> {
        const startTime = Date.now();

        while (Date.now() - startTime < timeoutMs) {
            try {
                const processInstance = await this.getProcessInstanceFromSecondary(processInstanceId);

                if (processInstance.status === expectedStatus) {
                    return processInstance;
                }

                // Wait a bit before next check
                await new Promise(resolve => setTimeout(resolve, 1000));
            } catch (error) {
                // If process instance not found yet, continue waiting
                if (Date.now() - startTime >= timeoutMs) {
                    throw error;
                }
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        }

        throw new Error(`Process instance ${processInstanceId} did not reach status ${expectedStatus} within ${timeoutMs}ms`);
    }
}
