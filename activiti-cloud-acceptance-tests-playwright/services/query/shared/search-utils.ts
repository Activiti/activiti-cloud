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

import { SearchPageParams } from '../../../models/base-service.models';
import { TaskSearchRequest } from '../../../models/task.models';

export function appendSearchPageParams(params: URLSearchParams, page?: SearchPageParams): void {
    if (!page) {
        return;
    }

    if (page.skipCount !== undefined) {
        params.set('skipCount', String(page.skipCount));
    }
    if (page.maxItems !== undefined) {
        params.set('maxItems', String(page.maxItems));
    }
    for (const sort of page.sort ?? []) {
        params.append('sort', sort);
    }
}

export function searchEndpoint(path: string, page?: SearchPageParams): string {
    if (!page) {
        return path;
    }

    const params = new URLSearchParams();
    appendSearchPageParams(params, page);
    const query = params.toString();
    return query ? `${path}?${query}` : path;
}

export function parseCountResponse(response: { body?: string }): number {
    const raw = response.body;
    if (raw === undefined || raw === '') {
        throw new Error('Unexpected empty count response');
    }
    const count = Number(raw);
    if (Number.isNaN(count)) {
        throw new Error(`Unexpected count response: ${raw}`);
    }
    return count;
}

export function toTaskSearchBody(searchRequest: TaskSearchRequest): TaskSearchRequest {
    return {
        onlyStandalone: false,
        onlyRoot: false,
        ...searchRequest,
    };
}
