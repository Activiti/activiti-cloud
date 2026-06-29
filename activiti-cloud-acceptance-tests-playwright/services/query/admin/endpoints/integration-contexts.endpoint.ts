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

import { CloudIntegrationContext } from '../../../../models/runtime-bundle.models';
import { BaseService } from '../../../base.service';
import { CustomAPIRequest } from '../../../../fixtures/context.models';
import { QUERY_ADMIN_V1_BASE } from './process-instances.endpoint';

export class QueryAdminIntegrationContextsEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getIntegrationContextAdmin(integrationContextId: string): Promise<CloudIntegrationContext> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(integrationContextId)}`);
        return this.unwrapEntity<CloudIntegrationContext>(response);
    }
}
