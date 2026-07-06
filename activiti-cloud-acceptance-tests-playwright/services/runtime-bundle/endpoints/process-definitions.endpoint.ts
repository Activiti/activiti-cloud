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

import { CloudProcessDefinition, ProcessDefinitionMeta } from '../../../models/process-definition.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService, RequestResponse } from '../../base.service';
import { rbV1Base } from './rb-base-path';

export class RbProcessDefinitionsEndpoint extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, runtimeBasePath: string = '/rb') {
        super(context);
        this.basePath = rbV1Base(runtimeBasePath);
    }

    async getProcessDefinitionById(processDefinitionId: string): Promise<CloudProcessDefinition> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}`);
        return this.unwrapEntity<CloudProcessDefinition>(response);
    }

    async getProcessDefinitionMeta(processDefinitionId: string): Promise<ProcessDefinitionMeta> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}/meta`);
        return this.unwrapEntity<ProcessDefinitionMeta>(response);
    }

    async getProcessDefinitionStaticValues(processDefinitionId: string): Promise<Record<string, unknown>> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}/static-values`);
        return this.unwrapMappingResponse(response);
    }

    async getProcessDefinitionConstantValues(processDefinitionId: string): Promise<Record<string, unknown>> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}/constant-values`);
        return this.unwrapMappingResponse(response);
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        const response = await this.get(`${this.basePath}/process-definitions`);
        const status = response.httpStatus;
        if (status === 401 || status === 403) {
            throw new Error(
                `Cannot list process definitions (HTTP ${status}). ` +
                    'Preview installs use seeded users (testuser/password) and client activiti with KEYCLOAK_CLIENT_SECRET from the namespace — not vars.KEYCLOAK_USERNAME / secrets.KEYCLOAK_PASSWORD.'
            );
        }
        return this.unwrapList<CloudProcessDefinition>(response, 'processDefinitions');
    }

    async getProcessDefinitionDiagram(processDefinitionId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-definitions/${processDefinitionId}/model`, {
            Accept: 'image/svg+xml',
        });
    }

    private unwrapMappingResponse(response: RequestResponse): Record<string, unknown> {
        const { httpStatus, body, ...rest } = response;
        if (httpStatus && httpStatus >= 400) {
            throw new Error(`Mapping values request failed (${httpStatus})`);
        }
        if (body && typeof body === 'object' && !Array.isArray(body)) {
            return body as Record<string, unknown>;
        }
        return rest as Record<string, unknown>;
    }
}
