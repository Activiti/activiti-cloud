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

import { test as playwrightTest } from '@playwright/test';
import { DirtyContextRegistry } from '../helpers/dirty-context';
import {
    createAuditService,
    createIdentityManagementService,
    createMultipleRuntimeBundleService,
    createQueryService,
    createRuntimeAdminService,
    createRuntimeBundleService,
    createSecurityPoliciesService,
    createTaskService,
    createTaskAdminService,
} from '../helpers/service-factory';
import { getTestScope, TestScope } from '../helpers/test-isolation';
import { MultipleRuntimeBundleService } from '../services/multiple-runtime-bundle.service';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { QueryService } from '../services/query/query.service';
import { RuntimeAdminService } from '../services/runtime-admin.service';
import { TaskService } from '../services/task.service';
import { TaskAdminService } from '../services/task-admin.service';
import { AuditService } from '../services/audit/audit.service';
import { SecurityPoliciesService } from '../services/security-policies.service';
import { IdentityManagementService } from '../services/identity-management.service';
import { contexts, expect } from './context.fixture';

interface ServicesFixture {
    testScope: TestScope;
    dirtyRegistry: DirtyContextRegistry;
    multipleRuntimeServiceTestUser: MultipleRuntimeBundleService;
    runtimeBundleServiceTestUser: RuntimeBundleService;
    queryServiceTestUser: QueryService;
    queryServiceHrUser: QueryService;
    queryAdminServiceProcessAdmin: QueryService;
    queryAdminServiceHrUser: QueryService;
    queryAdminServiceHradmin: QueryService;
    queryServiceTestAdmin: QueryService;
    runtimeAdminServiceTestAdmin: RuntimeAdminService;
    runtimeBundleServiceTestAdmin: RuntimeBundleService;
    runtimeBundleServiceHrUser: RuntimeBundleService;
    taskServiceTestUser: TaskService;
    taskServiceHrUser: TaskService;
    taskServiceTestAdmin: TaskService;
    taskAdminServiceTestAdmin: TaskAdminService;
    taskServiceHradmin: TaskService;
    taskAdminServiceHradmin: TaskAdminService;
    auditServiceTestUser: AuditService;
    auditServiceHrUser: AuditService;
    auditServiceTestAdmin: AuditService;
    auditAdminServiceTestAdmin: AuditService;
    auditAdminServiceHrUser: AuditService;
    anonymousQueryService: QueryService;
    anonymousQueryAdminService: QueryService;
    anonymousAuditService: AuditService;
    anonymousAuditAdminService: AuditService;
    invalidTokenQueryService: QueryService;
    invalidTokenQueryAdminService: QueryService;
    queryAdminServiceTestAdmin: QueryService;
    securityPoliciesServiceTestUser: SecurityPoliciesService;
    securityPoliciesServiceHrUser: SecurityPoliciesService;
    securityPoliciesServiceHradmin: SecurityPoliciesService;
    securityPoliciesServiceProcessAdmin: SecurityPoliciesService;
    identityManagementServiceTestUser: IdentityManagementService;
}

const isolationOpts = (dirtyRegistry: DirtyContextRegistry, testScope: TestScope) => ({
    dirtyRegistry,
    testScope,
});

const activiti = contexts.extend<ServicesFixture>({
    testScope: async ({}, use, testInfo) => {
        await use(getTestScope(testInfo));
    },

    dirtyRegistry: async ({}, use) => {
        const registry = new DirtyContextRegistry();
        await use(registry);
        const verbose = process.env.ACCEPTANCE_CLEANUP_VERBOSE?.trim().toLowerCase() === 'true';
        await registry.cleanup({
            step: playwrightTest.step,
            style: verbose ? 'console' : 'logger',
        });
    },

    multipleRuntimeServiceTestUser: async ({ testUserContext, dirtyRegistry, testScope }, use) => {
        await use(createMultipleRuntimeBundleService(testUserContext, isolationOpts(dirtyRegistry, testScope)));
    },
    runtimeBundleServiceTestUser: async ({ testUserContext, dirtyRegistry, testScope }, use) => {
        await use(createRuntimeBundleService(testUserContext, '/rb', isolationOpts(dirtyRegistry, testScope)));
    },
    queryServiceTestUser: async ({ testUserContext }, use) => {
        await use(createQueryService(testUserContext));
    },
    queryServiceHrUser: async ({ hrUserContext }, use) => {
        await use(createQueryService(hrUserContext));
    },
    queryAdminServiceProcessAdmin: async ({ processAdminContext }, use) => {
        await use(createQueryService(processAdminContext, true));
    },
    queryAdminServiceHrUser: async ({ hrUserContext }, use) => {
        await use(createQueryService(hrUserContext, true));
    },
    queryAdminServiceHradmin: async ({ hradminContext }, use) => {
        await use(createQueryService(hradminContext, true));
    },
    queryServiceTestAdmin: async ({ testAdminUserContext }, use) => {
        await use(createQueryService(testAdminUserContext));
    },
    runtimeAdminServiceTestAdmin: async ({ testAdminUserContext }, use) => {
        await use(createRuntimeAdminService(testAdminUserContext));
    },
    runtimeBundleServiceTestAdmin: async ({ testAdminUserContext, dirtyRegistry, testScope }, use) => {
        await use(createRuntimeBundleService(testAdminUserContext, '/rb', isolationOpts(dirtyRegistry, testScope)));
    },
    runtimeBundleServiceHrUser: async ({ hrUserContext, dirtyRegistry, testScope }, use) => {
        await use(createRuntimeBundleService(hrUserContext, '/rb', isolationOpts(dirtyRegistry, testScope)));
    },
    taskServiceTestUser: async ({ testUserContext, dirtyRegistry, testScope }, use) => {
        await use(createTaskService(testUserContext, isolationOpts(dirtyRegistry, testScope)));
    },
    taskServiceHrUser: async ({ hrUserContext, dirtyRegistry, testScope }, use) => {
        await use(createTaskService(hrUserContext, isolationOpts(dirtyRegistry, testScope)));
    },
    taskServiceTestAdmin: async ({ testAdminUserContext, dirtyRegistry, testScope }, use) => {
        await use(createTaskService(testAdminUserContext, isolationOpts(dirtyRegistry, testScope)));
    },
    taskAdminServiceTestAdmin: async ({ testAdminUserContext }, use) => {
        await use(createTaskAdminService(testAdminUserContext));
    },
    taskServiceHradmin: async ({ hradminContext, dirtyRegistry, testScope }, use) => {
        await use(createTaskService(hradminContext, isolationOpts(dirtyRegistry, testScope)));
    },
    taskAdminServiceHradmin: async ({ hradminContext }, use) => {
        await use(createTaskAdminService(hradminContext));
    },
    auditServiceTestUser: async ({ testUserContext }, use) => {
        await use(createAuditService(testUserContext));
    },
    auditServiceHrUser: async ({ hrUserContext }, use) => {
        await use(createAuditService(hrUserContext));
    },
    auditServiceTestAdmin: async ({ testAdminUserContext }, use) => {
        await use(createAuditService(testAdminUserContext));
    },
    auditAdminServiceTestAdmin: async ({ testAdminUserContext }, use) => {
        await use(createAuditService(testAdminUserContext));
    },
    auditAdminServiceHrUser: async ({ hrUserContext }, use) => {
        await use(createAuditService(hrUserContext));
    },
    anonymousQueryService: async ({ anonymousGatewayContext }, use) => {
        await use(createQueryService(anonymousGatewayContext));
    },
    anonymousQueryAdminService: async ({ anonymousGatewayContext }, use) => {
        await use(createQueryService(anonymousGatewayContext, true));
    },
    anonymousAuditService: async ({ anonymousGatewayContext }, use) => {
        await use(createAuditService(anonymousGatewayContext));
    },
    anonymousAuditAdminService: async ({ anonymousGatewayContext }, use) => {
        await use(createAuditService(anonymousGatewayContext));
    },
    invalidTokenQueryService: async ({ invalidTokenGatewayContext }, use) => {
        await use(createQueryService(invalidTokenGatewayContext));
    },
    invalidTokenQueryAdminService: async ({ invalidTokenGatewayContext }, use) => {
        await use(createQueryService(invalidTokenGatewayContext, true));
    },
    queryAdminServiceTestAdmin: async ({ testAdminUserContext }, use) => {
        await use(createQueryService(testAdminUserContext, true));
    },
    securityPoliciesServiceTestUser: async ({ testUserContext, dirtyRegistry, testScope }, use) => {
        await use(createSecurityPoliciesService(testUserContext, isolationOpts(dirtyRegistry, testScope)));
    },
    securityPoliciesServiceHrUser: async ({ hrUserContext, dirtyRegistry, testScope }, use) => {
        await use(createSecurityPoliciesService(hrUserContext, isolationOpts(dirtyRegistry, testScope)));
    },
    securityPoliciesServiceHradmin: async ({ hradminContext, dirtyRegistry, testScope }, use) => {
        await use(createSecurityPoliciesService(hradminContext, isolationOpts(dirtyRegistry, testScope)));
    },
    securityPoliciesServiceProcessAdmin: async ({ processAdminContext, dirtyRegistry, testScope }, use) => {
        await use(createSecurityPoliciesService(processAdminContext, isolationOpts(dirtyRegistry, testScope)));
    },
    identityManagementServiceTestUser: async ({ testUserContext }, use) => {
        await use(createIdentityManagementService(testUserContext));
    },
});

export { activiti, expect };
