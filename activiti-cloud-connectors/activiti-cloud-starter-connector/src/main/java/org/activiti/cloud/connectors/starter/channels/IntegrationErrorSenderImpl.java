/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class IntegrationErrorSenderImpl implements IntegrationErrorSender {

    private final IntegrationChannelResolver resolver;

    @Autowired
    public IntegrationErrorSenderImpl(IntegrationChannelResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void send(Message<IntegrationError> message) {
        IntegrationRequest request = message.getPayload().getIntegrationRequest();
        
        MessageChannel destination = resolver.resolveDestination(request);
        
        destination.send(message);
    }
}
