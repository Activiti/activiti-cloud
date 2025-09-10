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

import {
    CloudProcessInstance,
    StartProcessPayload,
    ProcessInstanceResponse,
    ProcessQueryParams
} from '../models/runtime-bundle.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../context.models';

export class RuntimeBundleService extends BaseService {
    private readonly basePath = '/rb/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async startProcess(payload: StartProcessPayload): Promise<CloudProcessInstance> {
        const response = await this.post(
            `${this.basePath}/process-instances`,
            { data: payload }
        );

        const result = response as ProcessInstanceResponse;
        return result.content;
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}`
        );

        const result = response as ProcessInstanceResponse;
        return result.content;
    }

    async getProcessInstances(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        const searchParams = new URLSearchParams();

        if (params?.status) searchParams.append('status', params.status);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.businessKey) searchParams.append('businessKey', params.businessKey);
        if (params?.name) searchParams.append('name', params.name);

        const response = await this.get(
            `${this.basePath}/process-instances?${searchParams.toString()}`
        );

        const result = response as any;
        return result.content || [];
    }
}
