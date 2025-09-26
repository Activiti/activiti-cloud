/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.services.connectors.behavior;

import java.util.Collection;
import java.util.Date;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.bpmn.behavior.DelegateExecutionFunction;
import org.activiti.engine.impl.bpmn.behavior.DelegateExecutionOutcome;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.DefaultServiceTaskBehavior;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.IntegrationRequestSender;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.enricher.IntegrationContextEnricher;

public class MQServiceTaskBehavior implements DelegateExecutionFunction {

    private final IntegrationContextManager integrationContextManager;
    private final IntegrationContextBuilder integrationContextBuilder;
    private final DefaultServiceTaskBehavior defaultServiceTaskBehavior;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final IntegrationRequestBuilder integrationRequestBuilder;
    private final IntegrationRequestSender integrationRequestSender;
    private final Collection<IntegrationContextEnricher> integrationContextEnrichers;

    public MQServiceTaskBehavior(
        IntegrationContextManager integrationContextManager,
        IntegrationRequestSender integrationRequestSender,
        IntegrationContextBuilder integrationContextBuilder,
        DefaultServiceTaskBehavior defaultServiceTaskBehavior,
        ProcessEngineEventsAggregator processEngineEventsAggregator,
        RuntimeBundleProperties runtimeBundleProperties,
        IntegrationRequestBuilder integrationRequestBuilder,
        Collection<IntegrationContextEnricher> integrationContextEnrichers
    ) {
        this.integrationContextManager = integrationContextManager;
        this.integrationRequestSender = integrationRequestSender;
        this.integrationContextBuilder = integrationContextBuilder;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.defaultServiceTaskBehavior = defaultServiceTaskBehavior;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.integrationContextEnrichers = integrationContextEnrichers;
    }

    @Override
    public DelegateExecutionOutcome apply(DelegateExecution execution) {
        if (defaultServiceTaskBehavior.hasConnectorBean(execution)) {
            // use de default implementation -> directly call a bean
            return defaultServiceTaskBehavior.apply(execution);
        }

        IntegrationContext integrationContext = integrationContextBuilder.from(
            storeIntegrationContext(execution),
            execution
        );
        enrichIntegrationContext(integrationContext);
        sendIntegrationRequest(integrationContext);
        aggregateCloudIntegrationRequestedEvent(integrationContext);

        return DelegateExecutionOutcome.WAIT_FOR_TRIGGER;
    }

    private void enrichIntegrationContext(IntegrationContext integrationContext) {
        if (integrationContextEnrichers != null) {
            integrationContextEnrichers.forEach(enricher -> enricher.enrich(integrationContext));
        }
    }

    private void aggregateCloudIntegrationRequestedEvent(IntegrationContext integrationContext) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationRequestedEventImpl cloudEvent = new CloudIntegrationRequestedEventImpl(integrationContext);

            processEngineEventsAggregator.add(cloudEvent);
        }
    }

    private void sendIntegrationRequest(IntegrationContext integrationContext) {
        IntegrationRequestImpl integrationRequest = integrationRequestBuilder.build(integrationContext);
        integrationRequestSender.sendIntegrationRequest(integrationRequest);
    }

    private IntegrationContextEntity storeIntegrationContext(DelegateExecution execution) {
        IntegrationContextEntity integrationContext = buildIntegrationContext(execution);
        integrationContextManager.insert(integrationContext);
        return integrationContext;
    }

    private IntegrationContextEntity buildIntegrationContext(DelegateExecution execution) {
        IntegrationContextEntity integrationContext = integrationContextManager.create();
        integrationContext.setExecutionId(execution.getId());
        integrationContext.setProcessInstanceId(execution.getProcessInstanceId());
        integrationContext.setProcessDefinitionId(execution.getProcessDefinitionId());
        integrationContext.setFlowNodeId(execution.getCurrentActivityId());
        integrationContext.setCreatedDate(new Date());
        return integrationContext;
    }
}
