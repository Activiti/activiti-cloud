/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import { HttpStatusCheck } from '../../models/base-service.models';
import { BaseService } from '../base.service';
import type { QueryService } from './query.service';
import { QUERY_ADMIN_V1_BASE, QUERY_V1_BASE } from './endpoints/index';

const defaultTaskSearchBody = (taskId: string) => ({
    onlyStandalone: false,
    onlyRoot: false,
    id: [taskId],
});

export class QueryStatusChecks {
    constructor(private readonly adminMode: boolean) {}

    buildUnauthenticatedGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        if (this.adminMode) {
            return [
                BaseService.getStatusCheck<QueryService>('tasks list', `${QUERY_ADMIN_V1_BASE}/tasks`),
                BaseService.getStatusCheck<QueryService>('process instances list', `${QUERY_ADMIN_V1_BASE}/process-instances`),
                BaseService.getStatusCheck<QueryService>(
                    'process instances with variable keys',
                    `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=start1`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance by id',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${fakeResourceId}`
                ),
                BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate groups',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-groups`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate users',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-users`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'task variables',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/variables`
                ),
                BaseService.getStatusCheck<QueryService>('applications list', `${QUERY_ADMIN_V1_BASE}/applications`),
                BaseService.getStatusCheck<QueryService>(
                    'integration context by id',
                    `${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(fakeResourceId)}`
                ),
            ];
        }
        return [
            BaseService.getStatusCheck<QueryService>('tasks list', `${QUERY_V1_BASE}/tasks`),
            BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
            BaseService.getStatusCheck<QueryService>(
                'process instance subprocesses',
                `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/subprocesses`
            ),
        ];
    }

    buildUnauthenticatedPostStatusChecks(
        fakeResourceId: string,
        linkType?: string
    ): readonly HttpStatusCheck<QueryService>[] {
        if (this.adminMode) {
            const taskSearchBody = defaultTaskSearchBody(fakeResourceId);
            return [
                BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                    id: [fakeResourceId],
                }),
                BaseService.postStatusCheck<QueryService>('process instance count', `${QUERY_ADMIN_V1_BASE}/process-instances/count`, {
                    id: [fakeResourceId],
                }),
                BaseService.postStatusCheck<QueryService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, taskSearchBody),
                BaseService.postStatusCheck<QueryService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, taskSearchBody),
            ];
        }
        const taskSearchBody = defaultTaskSearchBody(fakeResourceId);
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${QUERY_V1_BASE}/tasks/search`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('task count', `${QUERY_V1_BASE}/tasks/count`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_V1_BASE}/process-instances/search`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryService>('process instance count', `${QUERY_V1_BASE}/process-instances/count`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryService>(
                'process instance link',
                `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/link`,
                { processInstanceIds: [fakeResourceId], linkProcessInstanceType: linkType! }
            ),
        ];
    }

    buildNotFoundGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        if (this.adminMode) {
            return [
                BaseService.getStatusCheck<QueryService>(
                    'process instance by id',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${fakeResourceId}`
                ),
                BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate groups',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-groups`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate users',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-users`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'integration context by id',
                    `${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(fakeResourceId)}`
                ),
            ];
        }
        return [
            BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
            BaseService.getStatusCheck<QueryService>(
                'process instance subprocesses',
                `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/subprocesses`
            ),
        ];
    }

    buildNotFoundPostStatusChecks(fakeResourceId: string, linkType: string): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.postStatusCheck<QueryService>('process instance link', `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/link`, {
                processInstanceIds: [fakeResourceId],
                linkProcessInstanceType: linkType,
            }),
        ];
    }

    buildBadRequestPostStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        const base = this.adminMode ? QUERY_ADMIN_V1_BASE : QUERY_V1_BASE;
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${base}/tasks/search`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryService>('task count', `${base}/tasks/count`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryService>('process instance search', `${base}/process-instances/search`, {
                id: 'not-an-array',
            }),
        ];
    }

    buildForbiddenGetStatusChecks(taskId: string, processInstanceId?: string): readonly HttpStatusCheck<QueryService>[] {
        if (processInstanceId !== undefined) {
            return [
                BaseService.getStatusCheck<QueryService>('applications list', `${QUERY_ADMIN_V1_BASE}/applications`),
                BaseService.getStatusCheck<QueryService>('tasks list', `${QUERY_ADMIN_V1_BASE}/tasks`),
                BaseService.getStatusCheck<QueryService>('process instances list', `${QUERY_ADMIN_V1_BASE}/process-instances`),
                BaseService.getStatusCheck<QueryService>(
                    'process instances with variable keys',
                    `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=start1`
                ),
                BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}`),
                BaseService.getStatusCheck<QueryService>(
                    'task variables',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}/variables`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance variables',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/variables`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance sequence flows',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/sequence-flows`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance subprocesses',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/subprocesses`
                ),
            ];
        }
        return [
            BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}`),
            BaseService.getStatusCheck<QueryService>(
                'task candidate users',
                `${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}/candidate-users`
            ),
            BaseService.getStatusCheck<QueryService>(
                'task candidate groups',
                `${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}/candidate-groups`
            ),
        ];
    }

    buildForbiddenPostStatusChecks(taskId: string, fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        const taskSearchBody = defaultTaskSearchBody(taskId);
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryService>('process instance count', `${QUERY_ADMIN_V1_BASE}/process-instances/count`, {
                id: [fakeResourceId],
            }),
        ];
    }

    buildBadRequestLinkStatusChecks(
        mainProcessInstanceId: string,
        linkProcessInstanceType: string
    ): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.postStatusCheck<QueryService>(
                'process instance link',
                `${QUERY_V1_BASE}/process-instances/${mainProcessInstanceId}/link`,
                { processInstanceIds: 'invalid', linkProcessInstanceType }
            ),
        ];
    }
}
