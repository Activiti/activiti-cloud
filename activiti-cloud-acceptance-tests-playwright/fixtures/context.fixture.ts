/*
 * Copyright 2017-2026 Alfresco Software, Ltd.
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

import { test as base, expect } from '@playwright/test';
import type { UserKey } from '../config/users';
import { AuthCache } from './auth-cache';
import { CustomAPIRequest } from './context.models';

export interface UserContexts {
    hrUserContext: CustomAPIRequest;
    hradminContext: CustomAPIRequest;
    processAdminContext: CustomAPIRequest;
    testAdminUserContext: CustomAPIRequest;
    testUserContext: CustomAPIRequest;
}

export interface WorkerFixtures {
    authCache: AuthCache;
}

function userContext(userKey: UserKey) {
    return async ({ authCache }: { authCache: AuthCache }, use: (context: CustomAPIRequest) => Promise<void>) => {
        await use(await authCache.getContext(userKey));
    };
}

const contexts = base.extend<UserContexts, WorkerFixtures>({
    authCache: [
        async ({}, use) => {
            const cache = new AuthCache();
            await use(cache);
            await cache.disposeAll();
        },
        { scope: 'worker' },
    ],

    processAdminContext: userContext('processadmin'),
    hrUserContext: userContext('hruser'),
    hradminContext: userContext('hradmin'),
    testAdminUserContext: userContext('testAdminUser'),
    testUserContext: userContext('testUser'),
});

export { contexts, expect };
