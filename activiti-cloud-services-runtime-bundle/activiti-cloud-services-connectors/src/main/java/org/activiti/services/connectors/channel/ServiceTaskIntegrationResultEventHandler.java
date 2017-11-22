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

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.ApplicationProperties;
import org.activiti.cloud.services.events.integration.IntegrationResultReceivedEvent;
import org.activiti.cloud.services.events.integration.IntegrationResultReceivedEventImpl;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.model.IntegrationResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ProcessEngineIntegrationChannels.class)
public class ServiceTaskIntegrationResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationResultEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final ProcessEngineChannels channels;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ServiceTaskIntegrationResultEventHandler(RuntimeService runtimeService,
                                                    IntegrationContextService integrationContextService,
                                                    ProcessEngineChannels channels,
                                                    ApplicationProperties applicationProperties) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.channels = channels;
        this.applicationProperties = applicationProperties;
    }

    @StreamListener(ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER)
    public synchronized void receive(IntegrationResultEvent integrationResultEvent) {
        IntegrationContextEntity integrationContext = integrationContextService.findIntegrationContextByExecutionId(integrationResultEvent.getExecutionId());

        if (integrationContext != null) {
            integrationContextService.deleteIntegrationContext(integrationContext);
            runtimeService.trigger(integrationContext.getExecutionId(),
                                   integrationResultEvent.getVariables());

            Message<IntegrationResultReceivedEvent[]> message = MessageBuilder.withPayload(
                    new IntegrationResultReceivedEvent[]{
                            new IntegrationResultReceivedEventImpl(applicationProperties.getName(),
                                                                   integrationContext.getExecutionId(),
                                                                   integrationContext.getProcessDefinitionId(),
                                                                   integrationContext.getProcessInstanceId(),
                                                                   integrationContext.getId())
                    }).build();

            channels.auditProducer().send(message);
        } else {
            String message = "No task is waiting for integration result with execution id `" +
                    integrationResultEvent.getExecutionId() +
                    "`. The integration result `" + integrationResultEvent.getId() + "` will be ignored.";
            LOGGER.error( message );
            // This needs to throw an exception so the message goes back to the queue in case of other node can pick it up.
            throw new IllegalStateException(message);
        }
    }
}
