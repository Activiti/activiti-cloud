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
package org.activiti.services.connectors;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.enricher.IntegrationContextEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class IntegrationRequestReloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationRequestReloadService.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final IntegrationContextBuilder integrationContextBuilder;
    private final IntegrationRequestBuilder integrationRequestBuilder;
    private final Collection<IntegrationContextEnricher> integrationContextEnrichers;

    public IntegrationRequestReloadService(
        RuntimeService runtimeService,
        IntegrationContextService integrationContextService,
        IntegrationContextBuilder integrationContextBuilder,
        IntegrationRequestBuilder integrationRequestBuilder,
        Collection<IntegrationContextEnricher> integrationContextEnrichers
    ) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.integrationContextBuilder = integrationContextBuilder;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.integrationContextEnrichers = integrationContextEnrichers;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<IntegrationRequest> reload(String integrationContextId) {
        IntegrationContextEntity integrationContextEntity = integrationContextService.findById(integrationContextId);

        if (integrationContextEntity == null) {
            LOGGER.warn(
                "Unable to reload integration request because integration context '{}' no longer exists",
                integrationContextId
            );
            return Optional.empty();
        }

        String executionId = integrationContextEntity.getExecutionId();
        List<Execution> executions = runtimeService.createExecutionQuery().executionId(executionId).list();

        if (executions.isEmpty()) {
            LOGGER.warn(
                "Unable to reload integration request for integration context '{}' because execution '{}' was not found",
                integrationContextId,
                executionId
            );
            return Optional.empty();
        }

        Execution execution = executions.getFirst();
        if (!(execution instanceof ExecutionEntity executionEntity)) {
            LOGGER.warn(
                "Unable to reload integration request for integration context '{}' because execution '{}' is not an ExecutionEntity",
                integrationContextId,
                executionId
            );
            return Optional.empty();
        }

        IntegrationContext integrationContext = integrationContextBuilder.from(
            integrationContextEntity,
            executionEntity
        );
        integrationContextEnrichers.forEach(enricher -> enricher.enrich(integrationContext));

        return Optional.of(integrationRequestBuilder.build(integrationContext));
    }
}
