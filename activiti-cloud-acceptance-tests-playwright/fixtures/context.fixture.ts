/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { test as base, expect } from '@playwright/test';
import { ContextFactory } from '../context-factory';
import { UserContexts } from './context.model';

const activiti = base.extend<UserContexts>({
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

export { activiti, expect };
