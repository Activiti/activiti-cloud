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
package org.activiti.services.connectors.recovery;

import java.util.Optional;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the orphaned integration threshold of a service task, declared in the BPMN model with the field
 * {@code <activiti:field name="orphanedIntegrationThresholdSeconds" stringValue="1800"/>}.
 */
public class OrphanedIntegrationThresholdResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedIntegrationThresholdResolver.class);

    public static final String THRESHOLD_FIELD_NAME = "orphanedIntegrationThresholdSeconds";

    private final RepositoryService repositoryService;

    public OrphanedIntegrationThresholdResolver(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public Optional<Integer> resolveThresholdSeconds(String processDefinitionId, String flowNodeId) {
        return Optional.ofNullable(repositoryService.getBpmnModel(processDefinitionId))
            .map(bpmnModel -> bpmnModel.getFlowElement(flowNodeId))
            .filter(ServiceTask.class::isInstance)
            .map(ServiceTask.class::cast)
            .flatMap(serviceTask ->
                serviceTask
                    .getFieldExtensions()
                    .stream()
                    .filter(field -> THRESHOLD_FIELD_NAME.equals(field.getFieldName()))
                    .findFirst()
            )
            .map(FieldExtension::getStringValue)
            .flatMap(value -> toSeconds(value, flowNodeId));
    }

    private static Optional<Integer> toSeconds(String value, String flowNodeId) {
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException | NullPointerException _) {
            LOGGER.warn("Ignoring invalid {} '{}' of flow node {}.", THRESHOLD_FIELD_NAME, value, flowNodeId);
            return Optional.empty();
        }
    }
}
