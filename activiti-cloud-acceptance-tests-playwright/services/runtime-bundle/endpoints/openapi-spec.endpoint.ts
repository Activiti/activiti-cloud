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
import { BaseService } from '../../base.service';
import { rbV1Base } from './rb-base-path';

export class RbOpenApiSpecEndpoint extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, runtimeBasePath: string = '/rb') {
        super(context);
        this.basePath = rbV1Base(runtimeBasePath);
    }

    async getHomeInfo(): Promise<Record<string, unknown>> {
        const response = await this.get(this.basePath);
        return this.unwrapEntity<Record<string, unknown>>(response);
    }

    async getSwaggerSpecification(group: string = 'Runtime Bundle'): Promise<string> {
        const root = this.basePath.replace(/\/v1$/, '');
        return this.getText(`${root}/v3/api-docs/${encodeURIComponent(group)}`);
    }
}
