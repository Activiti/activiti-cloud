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

package org.activiti.cloud.starter.tests.runtime;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@TestComponent
public class IntegrationResultSender {

    private StreamBridge streamBridge;

    public IntegrationResultSender(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void send(IntegrationRequest integrationRequest, IntegrationContext integrationContext) {
        IntegrationResultImpl integrationResult = new IntegrationResultImpl(integrationRequest, integrationContext);
        Message<IntegrationResultImpl> message = MessageBuilder.withPayload(integrationResult).build();

        String destination = integrationRequest.getResultDestination();

        streamBridge.send(destination, message);
    }
}
