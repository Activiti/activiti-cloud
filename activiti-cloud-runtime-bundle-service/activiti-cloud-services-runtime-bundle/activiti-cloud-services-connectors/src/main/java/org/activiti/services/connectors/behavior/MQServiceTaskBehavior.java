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
package org.activiti.services.connectors.behavior;

import java.util.Date;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.delegate.TriggerableActivityBehavior;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.DefaultServiceTaskBehavior;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.IntegrationRequestSender;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class MQServiceTaskBehavior extends AbstractBpmnActivityBehavior implements TriggerableActivityBehavior {

    private final IntegrationContextManager integrationContextManager;
    private final ApplicationEventPublisher eventPublisher;
    private final IntegrationContextBuilder integrationContextBuilder;
    private final DefaultServiceTaskBehavior defaultServiceTaskBehavior;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final IntegrationRequestBuilder integrationRequestBuilder;

    public MQServiceTaskBehavior(
        IntegrationContextManager integrationContextManager,
        ApplicationEventPublisher eventPublisher,
        IntegrationContextBuilder integrationContextBuilder,
        DefaultServiceTaskBehavior defaultServiceTaskBehavior,
        ProcessEngineEventsAggregator processEngineEventsAggregator,
        RuntimeBundleProperties runtimeBundleProperties,
        IntegrationRequestBuilder integrationRequestBuilder
    ) {
        this.integrationContextManager = integrationContextManager;
        this.eventPublisher = eventPublisher;
        this.integrationContextBuilder = integrationContextBuilder;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.defaultServiceTaskBehavior = defaultServiceTaskBehavior;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
        this.runtimeBundleProperties = runtimeBundleProperties;
    }

    @Override
    public void execute(DelegateExecution execution) {
        if (defaultServiceTaskBehavior.hasConnectorBean(execution)) {
            // use de default implementation -> directly call a bean
            defaultServiceTaskBehavior.execute(execution);
        } else {
            IntegrationContextEntity integrationContextEntity = storeIntegrationContext(execution);

            IntegrationContext integrationContext = integrationContextBuilder.from(integrationContextEntity, execution);
            publishSpringEvent(integrationContext);

            aggregateCloudIntegrationRequestedEvent(integrationContext);
        }
    }

    private void aggregateCloudIntegrationRequestedEvent(IntegrationContext integrationContext) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationRequestedEventImpl cloudEvent = new CloudIntegrationRequestedEventImpl(integrationContext);

            processEngineEventsAggregator.add(cloudEvent);
        }
    }

    /**
     * Publishes an custom event using the Spring {@link ApplicationEventPublisher}. This event will be caught by
     * {@link IntegrationRequestSender#sendIntegrationRequest(IntegrationRequest)} which is annotated with
     * {@link TransactionalEventListener} on phase {@link TransactionPhase#AFTER_COMMIT}.
     *
     * @param integrationContext the related integration context
     */
    private void publishSpringEvent(IntegrationContext integrationContext) {
        IntegrationRequestImpl integrationRequest = integrationRequestBuilder.build(integrationContext);

        eventPublisher.publishEvent(integrationRequest);
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

    @Override
    public void trigger(DelegateExecution execution, String signalEvent, Object signalData) {
        leave(execution);
    }
}
