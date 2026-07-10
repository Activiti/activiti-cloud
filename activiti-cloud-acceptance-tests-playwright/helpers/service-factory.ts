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

import { CustomAPIRequest } from '../fixtures/context.models';
import { DirtyContextRegistry } from './dirty-context';
import { TestScope } from './test-isolation';
import { RuntimeBundleService } from '../services/runtime-bundle/runtime-bundle.service';
import { TaskService } from '../services/task/task.service';
import { SecurityPoliciesService } from '../services/security-policies.service';
import { MultipleRuntimeBundleService } from '../services/multiple-runtime-bundle.service';
import { QueryService } from '../services/query/query.service';
import { RuntimeAdminService } from '../services/runtime-admin/runtime-admin.service';
import { TaskAdminService } from '../services/task-admin/task-admin.service';
import { AuditService } from '../services/audit/audit.service';
import { IdentityManagementService } from '../services/identity-management.service';

export interface ServiceIsolationOptions {
    dirtyRegistry?: DirtyContextRegistry;
    testScope?: TestScope;
}

export function createRuntimeBundleService(
    context: CustomAPIRequest,
    runtimeBasePath = '/rb',
    isolation: ServiceIsolationOptions = {}
): RuntimeBundleService {
    const service = new RuntimeBundleService(context, runtimeBasePath);
    service.attachIsolation(isolation.dirtyRegistry, isolation.testScope);
    return service;
}

export function createTaskService(
    context: CustomAPIRequest,
    isolation: ServiceIsolationOptions = {}
): TaskService {
    const service = new TaskService(context);
    service.attachIsolation(isolation.dirtyRegistry, isolation.testScope);
    return service;
}

export function createSecurityPoliciesService(
    context: CustomAPIRequest,
    isolation: ServiceIsolationOptions = {}
): SecurityPoliciesService {
    const service = new SecurityPoliciesService(context);
    service.attachIsolation(isolation.dirtyRegistry, isolation.testScope);
    return service;
}

export function createMultipleRuntimeBundleService(
    context: CustomAPIRequest,
    isolation: ServiceIsolationOptions = {}
): MultipleRuntimeBundleService {
    const service = new MultipleRuntimeBundleService(context);
    service.attachIsolation(isolation.dirtyRegistry, isolation.testScope);
    return service;
}

export function createQueryService(context: CustomAPIRequest, adminMode = false): QueryService {
    return new QueryService(context, adminMode);
}

export function createRuntimeAdminService(
    context: CustomAPIRequest,
    isolation: ServiceIsolationOptions = {}
): RuntimeAdminService {
    const service = new RuntimeAdminService(context);
    service.attachIsolation(isolation.dirtyRegistry, isolation.testScope);
    return service;
}

export function createTaskAdminService(
    context: CustomAPIRequest,
    isolation: ServiceIsolationOptions = {}
): TaskAdminService {
    const service = new TaskAdminService(context);
    service.attachIsolation(isolation.dirtyRegistry, isolation.testScope);
    return service;
}

export function createAuditService(context: CustomAPIRequest): AuditService {
    return new AuditService(context);
}

export function createIdentityManagementService(context: CustomAPIRequest): IdentityManagementService {
    return new IdentityManagementService(context);
}
