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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.common.feature.FeatureToggle;
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
    FeatureToggle featureToggle;

    @Mock
    IntegrationContextQuery query;

    @Mock
    ConnectorTemplateResolver connectorTemplateResolver;

    private static final String AGENTIC_TEMPLATE = "agenticTaskConnector";

    OrphanedIntegrationRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OrphanedIntegrationRecoveryScheduler(
            integrationContextService,
            integrationRequestBuilder,
            errorEventHandler,
            0,
            Map.of(),
            featureToggle,
            connectorTemplateResolver
        );
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
                "Service task did not complete: the integration was not resolved within the expected time (threshold: 0 sec). " +
                    "Possible causes include application shutdown, connector crash, or task interruption."
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
    void should_queryWithThresholdBasedOnConstructorValue_when_recoveringOrphanedIntegrations() {
        var customScheduler = new OrphanedIntegrationRecoveryScheduler(
            integrationContextService,
            integrationRequestBuilder,
            errorEventHandler,
            300,
            Map.of(),
            featureToggle,
            connectorTemplateResolver
        );
        givenQuery(List.of());

        var expectedThreshold = Date.from(Instant.now().minus(300, ChronoUnit.SECONDS));
        customScheduler.recoverOrphanedIntegrations();

        var dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(query).createdBefore(dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isCloseTo(expectedThreshold, TimeUnit.SECONDS.toMillis(1));
    }

    @Test
    void should_queryWithLowestThreshold_when_connectorThresholdIsLowerThanGlobalOne() {
        var customScheduler = schedulerWithConnectorThresholds(
            10800,
            Map.of(AGENTIC_TEMPLATE, 600, "excludedConnector", -1)
        );
        givenQuery(List.of());

        var expectedThreshold = Date.from(Instant.now().minus(600, ChronoUnit.SECONDS));
        customScheduler.recoverOrphanedIntegrations();

        var dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(query).createdBefore(dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isCloseTo(expectedThreshold, TimeUnit.SECONDS.toMillis(1));
    }

    @Test
    void should_skipContext_when_connectorThresholdIsNegative() {
        var customScheduler = schedulerWithConnectorThresholds(0, Map.of(AGENTIC_TEMPLATE, -1));
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "agenticTask")));
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "agenticTask")).willReturn(
            Optional.of(AGENTIC_TEMPLATE)
        );

        customScheduler.recoverOrphanedIntegrations();

        verify(integrationRequestBuilder, never()).build(any());
        verify(errorEventHandler, never()).receive(any());
    }

    @Test
    void should_sendCloudBpmnError_when_elapsedTimeReachesConnectorThreshold() {
        var customScheduler = schedulerWithConnectorThresholds(10800, Map.of(AGENTIC_TEMPLATE, 600));
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "agenticTask", 700)));
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "agenticTask")).willReturn(
            Optional.of(AGENTIC_TEMPLATE)
        );
        givenBuilderReturnsRealRequest();

        customScheduler.recoverOrphanedIntegrations();

        var errorCaptor = ArgumentCaptor.forClass(IntegrationError.class);
        verify(errorEventHandler).receive(errorCaptor.capture());
        assertThat(errorCaptor.getValue().getErrorMessage()).contains("(threshold: 10 min)");
    }

    @Test
    void should_skipContext_when_elapsedTimeIsLowerThanConnectorThreshold() {
        var customScheduler = schedulerWithConnectorThresholds(0, Map.of(AGENTIC_TEMPLATE, 600));
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "agenticTask", 300)));
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "agenticTask")).willReturn(
            Optional.of(AGENTIC_TEMPLATE)
        );

        customScheduler.recoverOrphanedIntegrations();

        verify(errorEventHandler, never()).receive(any());
    }

    @Test
    void should_applyGlobalThreshold_when_templateHasNoConnectorThreshold() {
        var customScheduler = schedulerWithConnectorThresholds(10800, Map.of(AGENTIC_TEMPLATE, 600));
        givenQuery(
            List.of(
                entity("ctx-1", "exec-1", "proc-1", "procDef-1", "restTask", 700),
                entity("ctx-2", "exec-2", "proc-2", "procDef-2", "unknownTask", 10800)
            )
        );
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "restTask")).willReturn(
            Optional.of("restConnector")
        );
        given(connectorTemplateResolver.resolveTemplate("procDef-2", "unknownTask")).willReturn(Optional.empty());
        givenBuilderReturnsRealRequest();

        customScheduler.recoverOrphanedIntegrations();

        var contextCaptor = ArgumentCaptor.forClass(IntegrationContext.class);
        verify(integrationRequestBuilder).build(contextCaptor.capture());
        assertThat(contextCaptor.getValue().getId()).isEqualTo("ctx-2");
        verify(errorEventHandler).receive(any());
    }

    @Test
    void should_applyGlobalThreshold_when_connectorThresholdKeyDiffersInCase() {
        var customScheduler = schedulerWithConnectorThresholds(0, Map.of("agentictaskconnector", -1));
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "agenticTask")));
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "agenticTask")).willReturn(
            Optional.of(AGENTIC_TEMPLATE)
        );
        givenBuilderReturnsRealRequest();

        customScheduler.recoverOrphanedIntegrations();

        verify(errorEventHandler).receive(any());
    }

    @Test
    void should_ignoreConnectorThreshold_when_valueIsNull() {
        var connectorThresholds = new HashMap<String, Integer>();
        connectorThresholds.put(AGENTIC_TEMPLATE, null);
        var customScheduler = schedulerWithConnectorThresholds(300, connectorThresholds);
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "agenticTask", 300)));
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "agenticTask")).willReturn(
            Optional.of(AGENTIC_TEMPLATE)
        );
        givenBuilderReturnsRealRequest();

        customScheduler.recoverOrphanedIntegrations();

        verify(errorEventHandler).receive(any());
    }

    @Test
    void should_applyGlobalThreshold_when_templateResolutionFails() {
        var customScheduler = schedulerWithConnectorThresholds(300, Map.of(AGENTIC_TEMPLATE, -1));
        givenQuery(List.of(entity("ctx-1", "exec-1", "proc-1", "procDef-1", "agenticTask", 300)));
        given(connectorTemplateResolver.resolveTemplate("procDef-1", "agenticTask")).willThrow(
            new RuntimeException("model not found")
        );
        givenBuilderReturnsRealRequest();

        customScheduler.recoverOrphanedIntegrations();

        var errorCaptor = ArgumentCaptor.forClass(IntegrationError.class);
        verify(errorEventHandler).receive(errorCaptor.capture());
        assertThat(errorCaptor.getValue().getErrorMessage()).contains("(threshold: 5 min)");
    }

    private OrphanedIntegrationRecoveryScheduler schedulerWithConnectorThresholds(
        int thresholdSeconds,
        Map<String, Integer> connectorThresholds
    ) {
        return new OrphanedIntegrationRecoveryScheduler(
            integrationContextService,
            integrationRequestBuilder,
            errorEventHandler,
            thresholdSeconds,
            connectorThresholds,
            featureToggle,
            connectorTemplateResolver
        );
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
        return entity(id, executionId, processInstanceId, processDefinitionId, flowNodeId, 0);
    }

    private IntegrationContextEntityImpl entity(
        String id,
        String executionId,
        String processInstanceId,
        String processDefinitionId,
        String flowNodeId,
        int startedSecondsAgo
    ) {
        var e = new IntegrationContextEntityImpl();
        e.setId(id);
        e.setExecutionId(executionId);
        e.setProcessInstanceId(processInstanceId);
        e.setProcessDefinitionId(processDefinitionId);
        e.setFlowNodeId(flowNodeId);
        e.setCreatedDate(Date.from(Instant.now().minus(startedSecondsAgo, ChronoUnit.SECONDS)));
        return e;
    }
}
