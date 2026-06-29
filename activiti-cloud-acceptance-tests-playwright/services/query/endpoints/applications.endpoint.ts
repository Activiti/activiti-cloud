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
import { queryV1Base } from './query-base-path';

export class QueryApplicationsEndpoint extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, admin: boolean = false) {
        super(context);
        this.basePath = queryV1Base(admin);
    }

    async getApplications(): Promise<{ name: string; [key: string]: unknown }[]> {
        const response = await this.get(`${this.basePath}/applications`);
        return this.unwrapList<{ name: string; [key: string]: unknown }>(response, 'applications');
    }
}
