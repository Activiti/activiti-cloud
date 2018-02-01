/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.services.connectors.channel;

import java.util.List;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationResultReceivedEvent;
import org.activiti.cloud.services.events.integration.IntegrationResultReceivedEventImpl;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.model.IntegrationResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ProcessEngineIntegrationChannels.class)
public class ServiceTaskIntegrationResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationResultEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final MessageChannel auditProducer;
    private final RuntimeBundleProperties runtimeBundleProperties;

    public ServiceTaskIntegrationResultEventHandler(RuntimeService runtimeService,
                                                    IntegrationContextService integrationContextService,
                                                    MessageChannel auditProducer,
                                                    RuntimeBundleProperties runtimeBundleProperties) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.auditProducer = auditProducer;
        this.runtimeBundleProperties = runtimeBundleProperties;
    }

    @StreamListener(ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER)
    public void receive(IntegrationResultEvent integrationResultEvent) {
        List<IntegrationContextEntity> integrationContexts = integrationContextService.findIntegrationContextByExecutionId(integrationResultEvent.getExecutionId());

        if (integrationContexts == null || integrationContexts.size() == 0) {
            LOGGER.debug("No integration contexts found in this RB for execution Id `" + integrationResultEvent.getExecutionId() +
                                ", flow node id `" + integrationResultEvent.getFlowNodeId() + "`");
        }

        if (integrationContexts != null) {
            for (IntegrationContextEntity integrationContext : integrationContexts) {
                if (integrationContext != null) {
                    integrationContextService.deleteIntegrationContext(integrationContext);
                }
                sendAuditMessage(integrationContext);
            }
        }

        if (runtimeService.createExecutionQuery().executionId(integrationResultEvent.getExecutionId()).list().size() > 0) {
            runtimeService.trigger(integrationResultEvent.getExecutionId(),
                                   integrationResultEvent.getVariables());
        } else {
            String message = "No task is in this RB is waiting for integration result with execution id `" +
                    integrationResultEvent.getExecutionId() +
                    ", flow node id `" + integrationResultEvent.getFlowNodeId() +
                    "`. The integration result `" + integrationResultEvent.getId() + "` will be ignored.";
            LOGGER.debug(message);
        }
    }

    private void sendAuditMessage(IntegrationContextEntity integrationContext) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            Message<IntegrationResultReceivedEvent[]> message = MessageBuilder.withPayload(
                    new IntegrationResultReceivedEvent[]{
                            new IntegrationResultReceivedEventImpl(runtimeBundleProperties.getName(),
                                                                   integrationContext.getExecutionId(),
                                                                   integrationContext.getProcessDefinitionId(),
                                                                   integrationContext.getProcessInstanceId(),
                                                                   integrationContext.getId(),
                                                                   integrationContext.getFlowNodeId())
                    }).build();

            auditProducer.send(message);
        }
    }
}
