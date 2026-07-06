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

import {
    CloudProcessInstance,
    ProcessQueryParams,
    UpdateProcessPayload,
} from '../../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../../models/process-definition.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { BaseService, RequestResponse } from '../base.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import {
    RbAdminExecutionsEndpoint,
    RbAdminProcessDefinitionsEndpoint,
    RbAdminProcessInstancesEndpoint,
} from './endpoints/index';

export class RuntimeAdminService extends BaseService {
    readonly processInstances: RbAdminProcessInstancesEndpoint;
    readonly processDefinitions: RbAdminProcessDefinitionsEndpoint;
    readonly executions: RbAdminExecutionsEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.processInstances = new RbAdminProcessInstancesEndpoint(context);
        this.processDefinitions = new RbAdminProcessDefinitionsEndpoint(context);
        this.executions = new RbAdminExecutionsEndpoint(context);
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return this.processInstances.getAllProcessInstances();
    }

    async getProcessInstancesWithParams(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        return this.processInstances.getProcessInstancesWithParams(params);
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstances.getProcessInstance(processInstanceId);
    }

    async updateProcessInstance(
        processInstanceId: string,
        payload: Omit<UpdateProcessPayload, 'payloadType'>
    ): Promise<CloudProcessInstance> {
        return this.processInstances.updateProcessInstance(processInstanceId, payload);
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        return this.processInstances.getProcessInstanceVariables(processInstanceId);
    }

    async suspendProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstances.suspendProcessInstance(processInstanceId);
    }

    async resumeProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstances.resumeProcessInstance(processInstanceId);
    }

    async getSubProcesses(parentProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        return this.processInstances.getSubProcesses(parentProcessInstanceId);
    }

    async sendStartMessage(payload: {
        name: string;
        businessKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<CloudProcessInstance> {
        return this.processInstances.sendStartMessage(payload);
    }

    async sendReceiveMessage(payload: {
        name: string;
        correlationKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<RequestResponse> {
        return this.processInstances.sendReceiveMessage(payload);
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        return this.processDefinitions.getProcessDefinitions();
    }

    async deleteProcessInstance(processInstanceId: string): Promise<void> {
        return this.processInstances.deleteProcessInstance(processInstanceId);
    }

    async destroyProcessInstance(processInstanceId: string, force = true): Promise<void> {
        return this.processInstances.destroyProcessInstance(processInstanceId, force);
    }

    async replayServiceTask(executionId: string, flowNodeId: string): Promise<RequestResponse> {
        return this.executions.replayServiceTask(executionId, flowNodeId);
    }

    async setProcessVariables(processInstanceId: string, variables: Record<string, unknown>): Promise<void> {
        return this.processInstances.setProcessVariables(processInstanceId, variables);
    }

    async deleteProcessVariables(processInstanceId: string, variableNames: string[]): Promise<void> {
        return this.processInstances.deleteProcessVariables(processInstanceId, variableNames);
    }
}
