/*
 * Factory helpers — attach dirty-context + test scope to services consistently.
 */

import { CustomAPIRequest } from '../context.models';
import { DirtyContextRegistry } from './dirty-context';
import { TestScope } from './test-isolation';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { TaskService } from '../services/task.service';
import { SecurityPoliciesService } from '../services/security-policies.service';
import { MultipleRuntimeBundleService } from '../services/multiple-runtime-bundle.service';
import { QueryService } from '../services/query.service';
import { QueryAdminService } from '../services/query-admin.service';
import { RuntimeAdminService } from '../services/runtime-admin.service';
import { TaskAdminService } from '../services/task-admin.service';
import { AuditService } from '../services/audit.service';

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

export function createQueryService(context: CustomAPIRequest): QueryService {
    return new QueryService(context);
}

export function createQueryAdminService(context: CustomAPIRequest): QueryAdminService {
    return new QueryAdminService(context);
}

export function createRuntimeAdminService(context: CustomAPIRequest): RuntimeAdminService {
    return new RuntimeAdminService(context);
}

export function createTaskAdminService(context: CustomAPIRequest): TaskAdminService {
    return new TaskAdminService(context);
}

export function createAuditService(context: CustomAPIRequest): AuditService {
    return new AuditService(context);
}
