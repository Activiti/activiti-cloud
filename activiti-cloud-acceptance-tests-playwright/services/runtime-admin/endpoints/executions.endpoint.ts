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

import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService, RequestResponse } from '../../base.service';
import { RB_ADMIN_V1_BASE } from '../../runtime-bundle/endpoints/rb-base-path';

export class RbAdminExecutionsEndpoint extends BaseService {
    private readonly basePath = RB_ADMIN_V1_BASE;

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async replayServiceTask(executionId: string, flowNodeId: string): Promise<RequestResponse> {
        return this.post(`${this.basePath}/executions/${executionId}/replay/service-task`, {
            data: { flowNodeId },
        });
    }
}
