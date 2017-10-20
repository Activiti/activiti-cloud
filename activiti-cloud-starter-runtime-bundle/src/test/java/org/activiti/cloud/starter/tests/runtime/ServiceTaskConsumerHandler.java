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

package org.activiti.cloud.starter.tests.runtime;

import java.util.UUID;

import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.activiti.services.connectors.model.IntegrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ConnectorIntegrationChannels.class)
public class ServiceTaskConsumerHandler {

    @Autowired
    private ConnectorIntegrationChannels consumerChannels;

    @StreamListener(value = ConnectorIntegrationChannels.INTEGRATION_EVENTS_CONSUMER, condition = "headers['connectorType']=='payment'")
    public synchronized void receive(IntegrationRequestEvent integrationRequestEvent) {
        integrationRequestEvent.putVariable("age",
                                            42);
        Message<IntegrationResult> message = MessageBuilder.withPayload(new IntegrationResult(UUID.randomUUID().toString(),
                                                                                              integrationRequestEvent.getExecutionId(),
                                                                                              integrationRequestEvent.getVariables())).build();
        consumerChannels.integrationResultsProducer().send(message);
    }
}
