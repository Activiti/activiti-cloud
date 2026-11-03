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
package org.activiti.services.connectors.channel;

import java.util.Optional;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessVariablesMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves whether the variables mapping of a service task is ephemeral.
 * <p>
 * The flag is read from the process extensions (mapping) deployed in this runtime bundle,
 * using the process definition id and flow node id stored in the runtime bundle's own
 * integration context. It never relies on the flag sent back by the connector.
 */
public class EphemeralMappingResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralMappingResolver.class);

    private final ProcessExtensionService processExtensionService;

    public EphemeralMappingResolver(ProcessExtensionService processExtensionService) {
        this.processExtensionService = processExtensionService;
    }

    public boolean isEphemeral(IntegrationContextEntity integrationContextEntity) {
        if (integrationContextEntity == null) {
            return false;
        }
        return isEphemeral(integrationContextEntity.getProcessDefinitionId(), integrationContextEntity.getFlowNodeId());
    }

    public boolean isEphemeral(String processDefinitionId, String flowNodeId) {
        if (processDefinitionId == null || flowNodeId == null) {
            return false;
        }
        try {
            return Optional.ofNullable(processExtensionService.getExtensionsForId(processDefinitionId))
                .map(Extension::getMappings)
                .map(mappings -> mappings.get(flowNodeId))
                .map(ProcessVariablesMapping::isEphemeral)
                .orElse(false);
        } catch (RuntimeException e) {
            // Fail closed: if the mapping cannot be resolved, do not leak outbound variables in audit events
            LOGGER.warn(
                "Unable to resolve ephemeral flag for process definition '{}' and flow node '{}'. " +
                    "Treating the mapping as ephemeral.",
                processDefinitionId,
                flowNodeId,
                e
            );
            return true;
        }
    }
}
