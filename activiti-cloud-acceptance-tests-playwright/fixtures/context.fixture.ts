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


import { test as base, expect } from '@playwright/test';
import { ContextFactory } from '../context-factory';
import { CustomAPIRequest } from '../context.models';

interface UserContexts {
    hrUserContext: CustomAPIRequest;
    processAdminContext: CustomAPIRequest;
    modelerUserContext: CustomAPIRequest;
    modelerqaUserContext: CustomAPIRequest;
    devopsUserContext: CustomAPIRequest;
    superadminContext: CustomAPIRequest;
    salesUserContext: CustomAPIRequest;
    testAdminUserContext: CustomAPIRequest;
    testUserContext: CustomAPIRequest;
}

const contexts = base.extend<UserContexts>({
    processAdminContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('processadmin'));
    },
    devopsUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('devopsuser'));
    },
    hrUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('hruser'));
    },
    modelerUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('modeler'));
    },
    modelerqaUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('modelerqa'));
    },
    superadminContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('superadminuser'));
    },
    salesUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('salesUser'));
    },
    testAdminUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('testAdminUser'));
    },
    testUserContext: async ({}, use) => {
        await use(await ContextFactory.getContextByUserName('testUser'));
    }
});

export { contexts, expect };
