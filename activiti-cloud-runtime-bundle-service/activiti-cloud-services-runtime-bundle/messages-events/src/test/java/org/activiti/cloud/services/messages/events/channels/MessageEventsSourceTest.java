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
package org.activiti.cloud.services.messages.events.channels;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.cloud.services.messages.events.Application;
import org.activiti.cloud.services.messages.events.support.MessageEventsDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = { Application.class })
@Import({ TestChannelBinderConfiguration.class })
@ActiveProfiles("binding")
class MessageEventsSourceTest {

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private MessageEventsDispatcher messageEventsDispatcher;

    @Test
    public void shouldHaveChannelBindingsSetForMessageEventsProducer() {
        //given
        String destination = "messageEvents";

        //when
        TransactionSynchronizationManager.initSynchronization();
        try {
            messageEventsDispatcher.dispatch(createMessagePayload());
            TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        //then
        Message<byte[]> received = outputDestination.receive(0l, destination);
        assertNotNull(received);
    }

    private Message<MessageEventPayload> createMessagePayload() {
        MessageEventPayload messagePayload = MessagePayloadBuilder.event("messageName")
                .withBusinessKey("businessId")
                .withCorrelationKey("correlationId")
                .withVariable("name", "value")
                .build();

        return new GenericMessage<>(messagePayload);
    }
}