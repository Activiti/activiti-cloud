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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextQuery;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrphanedIntegrationRecoverySchedulerTest {

    @Mock
    IntegrationContextService integrationContextService;

    @Mock
    IntegrationRequestBuilder integrationRequestBuilder;

    @Mock
    ServiceTaskIntegrationErrorEventHandler errorEventHandler;

    @Mock
    OrphanedIntegrationRecoveryProperties properties;

    @Mock
    FeatureToggle featureToggle;

    @Mock
    IntegrationContextQuery query;

    @InjectMocks
    OrphanedIntegrationRecoveryScheduler scheduler;

    @BeforeEach
    void enableFeatureToggle() {
        given(featureToggle.isEnabled(RuntimeBundleFeatureToggles.ORPHANED_INTEGRATION_RECOVERY)).willReturn(true);
    }

    @Test
    void should_doNothing_when_featureToggleIsDisabled() {
        given(featureToggle.isEnabled(RuntimeBundleFeatureToggles.ORPHANED_INTEGRATION_RECOVERY)).willReturn(false);

        scheduler.recoverOrphanedIntegrations();

        verify(integrationContextService, never()).createIntegrationContextQuery();
        verify(errorEventHandler, never()).receive(any());
    }

    @Test
    void should_doNothing_when_noOrphanedIntegrationsFound() {
        givenQuery(List.of());

        scheduler.recoverOrphanedIntegrations();

        verify(errorEventHandler, never()).receive(any());
    }

    @Test
    void should_sendCloudBpmnError_when_orphanedIntegrationIsFound() {
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "ServiceTask")));
        givenBuilderReturnsRealRequest();

        scheduler.recoverOrphanedIntegrations();

        var errorCaptor = ArgumentCaptor.forClass(IntegrationError.class);
        verify(errorEventHandler).receive(errorCaptor.capture());
        assertThat(errorCaptor.getValue()).satisfies(error -> {
            assertThat(error.getErrorClassName()).isEqualTo(CloudBpmnError.class.getName());
            assertThat(error.getErrorMessage()).isEqualTo(
                OrphanedIntegrationRecoveryScheduler.ORPHANED_INTEGRATION_ERROR_MESSAGE
            );
        });
    }

    @Test
    void should_populateIntegrationContextFromEntity_when_buildingIntegrationRequest() {
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "ServiceTask")));
        givenBuilderReturnsRealRequest();

        scheduler.recoverOrphanedIntegrations();

        var contextCaptor = ArgumentCaptor.forClass(IntegrationContext.class);
        verify(integrationRequestBuilder).build(contextCaptor.capture());
        assertThat(contextCaptor.getValue()).satisfies(ctx -> {
            assertThat(ctx.getId()).isEqualTo("ctx-1");
            assertThat(ctx.getExecutionId()).isEqualTo("exec-1");
            assertThat(ctx.getProcessInstanceId()).isEqualTo("proc-1");
            assertThat(ctx.getProcessDefinitionId()).isEqualTo("procDef-1");
            assertThat(ctx.getClientId()).isEqualTo("ServiceTask");
        });
    }

    @Test
    void should_continueRecoveringRemainingContexts_when_oneRecoveryThrows() {
        givenQuery(
            List.of(
                entity("ctx-1", "exec-1", "proc-1", "procDef-1", "Task"),
                entity("ctx-2", "exec-2", "proc-2", "procDef-2", "Task")
            )
        );
        givenBuilderReturnsRealRequest();
        willThrow(new RuntimeException("handler failure")).given(errorEventHandler).receive(any());

        scheduler.recoverOrphanedIntegrations();

        verify(errorEventHandler, times(2)).receive(any());
    }

    @Test
    void should_queryWithThresholdBasedOnProperties_when_recoveringOrphanedIntegrations() {
        given(properties.getThresholdSeconds()).willReturn(300);
        givenQuery(List.of());

        var expectedThreshold = Date.from(Instant.now().minus(300, ChronoUnit.SECONDS));
        scheduler.recoverOrphanedIntegrations();

        var dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(query).createdBefore(dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isCloseTo(expectedThreshold, TimeUnit.SECONDS.toMillis(1));
    }

    private void givenQuery(List<IntegrationContextEntity> entities) {
        given(integrationContextService.createIntegrationContextQuery()).willReturn(query);
        given(query.createdBefore(any())).willReturn(query);
        given(query.list()).willReturn(entities);
    }

    private void givenBuilderReturnsRealRequest() {
        willAnswer(inv -> new IntegrationRequestImpl((IntegrationContext) inv.getArgument(0)))
            .given(integrationRequestBuilder)
            .build(any());
    }

    private IntegrationContextEntityImpl entity(
        String id,
        String executionId,
        String processInstanceId,
        String processDefinitionId,
        String flowNodeId
    ) {
        var e = new IntegrationContextEntityImpl();
        e.setId(id);
        e.setExecutionId(executionId);
        e.setProcessInstanceId(processInstanceId);
        e.setProcessDefinitionId(processDefinitionId);
        e.setFlowNodeId(flowNodeId);
        return e;
    }
}
