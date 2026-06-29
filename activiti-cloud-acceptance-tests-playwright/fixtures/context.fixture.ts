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

import { test as base, expect } from '@michalfidor/playswag';
import type { UserKey } from '../config/users';
import { AuthCache } from './auth-cache';
import { wrapAuthenticatedApiContext } from './context-factory';
import { CustomAPIRequest } from './context.models';

interface UserContexts {
    hrUserContext: CustomAPIRequest;
    hradminContext: CustomAPIRequest;
    processAdminContext: CustomAPIRequest;
    modelerUserContext: CustomAPIRequest;
    modelerqaUserContext: CustomAPIRequest;
    devopsUserContext: CustomAPIRequest;
    superadminContext: CustomAPIRequest;
    salesUserContext: CustomAPIRequest;
    testAdminUserContext: CustomAPIRequest;
    testUserContext: CustomAPIRequest;
}

interface WorkerFixtures {
    authCache: AuthCache;
}

function userContext(userKey: UserKey) {
    return async (
        { authCache, trackRequest }: { authCache: AuthCache; trackRequest: (ctx: CustomAPIRequest) => CustomAPIRequest },
        use: (context: CustomAPIRequest) => Promise<void>
    ) => {
        const cached = await authCache.getContext(userKey);
        const tracked = trackRequest(cached);
        await use(wrapAuthenticatedApiContext(tracked, cached.token, cached.expires_in, cached.username));
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
    devopsUserContext: userContext('devopsuser'),
    hrUserContext: userContext('hruser'),
    hradminContext: userContext('hradmin'),
    modelerUserContext: userContext('modeler'),
    modelerqaUserContext: userContext('modelerqa'),
    superadminContext: userContext('superadminuser'),
    salesUserContext: userContext('salesUser'),
    testAdminUserContext: userContext('testAdminUser'),
    testUserContext: userContext('testUser'),
});

export { contexts, expect };
