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
package org.activiti.services.connectors.enricher;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;

public class ServiceTaskTypeIntegrationContextEnricher implements IntegrationContextEnricher {

    private final ProcessExtensionService processExtensionService;

    public ServiceTaskTypeIntegrationContextEnricher(ProcessExtensionService processExtensionService) {
        this.processExtensionService = processExtensionService;
    }

    @Override
    public void enrich(IntegrationContext integrationContext) {
        if (!(integrationContext instanceof IntegrationContextImpl mutableContext)) {
            return;
        }
        Extension extensions = processExtensionService.getExtensionsForId(integrationContext.getProcessDefinitionId());
        if (extensions == null) {
            return;
        }
        String serviceTaskType = extensions.getServiceTaskType(integrationContext.getClientId());
        if (serviceTaskType != null) {
            mutableContext.setServiceTaskType(serviceTaskType);
        }
    }
}
