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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.enricher.IntegrationContextEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationRequestReloadServiceTest {

    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";
    private static final String EXECUTION_ID = "executionId";

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private IntegrationContextService integrationContextService;

    @Mock
    private IntegrationContextBuilder integrationContextBuilder;

    @Mock
    private IntegrationRequestBuilder integrationRequestBuilder;

    @Mock
    private IntegrationContextEnricher integrationContextEnricher;

    @Mock
    private IntegrationContextEntity integrationContextEntity;

    @Mock
    private IntegrationContext integrationContext;

    @Mock
    private IntegrationRequestImpl integrationRequest;

    @Mock
    private ExecutionQuery executionQuery;

    private IntegrationRequestReloadService integrationRequestReloadService;

    @BeforeEach
    void setUp() {
        integrationRequestReloadService = new IntegrationRequestReloadService(
            runtimeService,
            integrationContextService,
            integrationContextBuilder,
            integrationRequestBuilder,
            List.of(integrationContextEnricher)
        );

        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId(EXECUTION_ID)).thenReturn(executionQuery);
        when(integrationContextEntity.getExecutionId()).thenReturn(EXECUTION_ID);
    }

    @Test
    void shouldReloadIntegrationRequest() {
        ExecutionEntity executionEntity = mock(ExecutionEntity.class);

        when(integrationContextService.findById(INTEGRATION_CONTEXT_ID)).thenReturn(integrationContextEntity);
        when(executionQuery.list()).thenReturn(List.of(executionEntity));
        when(integrationContextBuilder.from(integrationContextEntity, executionEntity)).thenReturn(integrationContext);
        when(integrationRequestBuilder.build(integrationContext)).thenReturn(integrationRequest);

        var reloaded = integrationRequestReloadService.reload(INTEGRATION_CONTEXT_ID);

        assertThat(reloaded).containsSame(integrationRequest);
        verify(integrationContextEnricher).enrich(integrationContext);
    }

    @Test
    void shouldReturnEmptyWhenContextIsMissing() {
        when(integrationContextService.findById(INTEGRATION_CONTEXT_ID)).thenReturn(null);

        var reloaded = integrationRequestReloadService.reload(INTEGRATION_CONTEXT_ID);

        assertThat(reloaded).isEmpty();
        verify(runtimeService, never()).createExecutionQuery();
    }

    @Test
    void shouldReturnEmptyWhenExecutionIsMissing() {
        when(integrationContextService.findById(INTEGRATION_CONTEXT_ID)).thenReturn(integrationContextEntity);
        when(executionQuery.list()).thenReturn(List.of());

        var reloaded = integrationRequestReloadService.reload(INTEGRATION_CONTEXT_ID);

        assertThat(reloaded).isEmpty();
        verify(integrationContextBuilder, never()).from(any(), any());
    }
}
