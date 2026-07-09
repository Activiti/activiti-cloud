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

import { CustomAPIRequest } from '../../fixtures/context.models';
import { DirtyContextRegistry } from '../../helpers/dirty-context';
import { TestScope } from '../../helpers/test-isolation';
import { RB_ADMIN_V1_BASE } from '../runtime-bundle/endpoints/rb-base-path';
import { BaseService } from '../base.service';
import { RbAdminTasksEndpoint } from './endpoints/tasks.endpoint';

export class TaskAdminService extends BaseService {
    readonly tasks: RbAdminTasksEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.tasks = new RbAdminTasksEndpoint(context);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope): void {
        super.attachIsolation(dirtyRegistry, testScope);
        this.tasks.attachIsolation(dirtyRegistry, testScope, RB_ADMIN_V1_BASE);
    }
}
