/*
 * Shared URLSearchParams builders for runtime/query task and process list endpoints.
 */

import { ProcessQueryParams } from '../models/runtime-bundle.models';
import { TaskQueryParams } from '../models/task.models';

export function appendProcessQueryParams(
    searchParams: URLSearchParams,
    params?: ProcessQueryParams
): void {
    if (!params) {
        return;
    }
    if (params.status) {
        searchParams.append('status', params.status);
    }
    if (params.processDefinitionKey) {
        searchParams.append('processDefinitionKey', params.processDefinitionKey);
    }
    if (params.businessKey) {
        searchParams.append('businessKey', params.businessKey);
    }
    if (params.name) {
        searchParams.append('name', params.name);
    }
}

export function toProcessQueryString(params?: ProcessQueryParams): string {
    const searchParams = new URLSearchParams();
    appendProcessQueryParams(searchParams, params);
    return searchParams.toString();
}

export function appendTaskQueryParams(searchParams: URLSearchParams, params?: TaskQueryParams): void {
    if (!params) {
        return;
    }
    if (params.status) {
        searchParams.append('status', params.status);
    }
    if (params.assignee) {
        searchParams.append('assignee', params.assignee);
    }
    if (params.owner) {
        searchParams.append('owner', params.owner);
    }
    if (params.processInstanceId) {
        searchParams.append('processInstanceId', params.processInstanceId);
    }
    if (params.processDefinitionKey) {
        searchParams.append('processDefinitionKey', params.processDefinitionKey);
    }
    if (params.name) {
        searchParams.append('name', params.name);
    }
}

export function toTaskQueryString(params?: TaskQueryParams): string {
    const searchParams = new URLSearchParams();
    appendTaskQueryParams(searchParams, params);
    return searchParams.toString();
}

export function toServiceTaskQueryString(params?: {
    processDefinitionKey?: string;
    status?: string;
    processInstanceId?: string;
}): string {
    const searchParams = new URLSearchParams();
    if (params?.processDefinitionKey) {
        searchParams.append('processDefinitionKey', params.processDefinitionKey);
    }
    if (params?.status) {
        searchParams.append('status', params.status);
    }
    if (params?.processInstanceId) {
        searchParams.append('processInstanceId', params.processInstanceId);
    }
    return searchParams.toString();
}
