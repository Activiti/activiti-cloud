/*
 * Service task and integration context models (query admin + runtime admin).
 */

export type ServiceTaskStatus = 'STARTED' | 'COMPLETED' | 'ERROR' | 'CANCELLED';

export interface CloudServiceTask {
    id: string;
    activityType?: string;
    status?: ServiceTaskStatus | string;
    processInstanceId?: string;
    processDefinitionId?: string;
    processDefinitionKey?: string;
    integrationContextCounter?: number;
    [key: string]: unknown;
}

export type IntegrationContextStatus =
    | 'INTEGRATION_REQUESTED'
    | 'INTEGRATION_RESULT_RECEIVED'
    | 'INTEGRATION_ERROR_RECEIVED'
    | string;

export interface CloudIntegrationContext {
    clientType?: string;
    status?: IntegrationContextStatus;
    executionId?: string;
    clientId?: string;
    processDefinitionId?: string;
    processInstanceId?: string;
    [key: string]: unknown;
}

export interface ServiceTaskQueryParams {
    processDefinitionKey?: string;
    status?: ServiceTaskStatus | string;
    processInstanceId?: string;
}
