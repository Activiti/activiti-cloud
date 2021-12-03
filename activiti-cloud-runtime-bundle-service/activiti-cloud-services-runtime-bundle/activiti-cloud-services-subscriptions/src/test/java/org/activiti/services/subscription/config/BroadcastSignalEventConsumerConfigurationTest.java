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
package org.activiti.services.subscription.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import java.util.Map;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.engine.RuntimeService;
import org.activiti.runtime.api.signal.SignalPayloadEventListener;
import org.activiti.services.subscription.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = { Application.class })
@Import({ TestChannelBinderConfiguration.class })
@ActiveProfiles("binding")
class BroadcastSignalEventConsumerConfigurationTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private SignalPayloadEventListener signalPayloadEventListener;

    @MockBean
    private RuntimeService runtimeService;

    @Test
    public void shouldHaveChannelBindingsSetForSignalConsumer() {
        //given
        String signalName = "signal";
        Message<SignalPayload> message = new GenericMessage<>(new SignalPayload(signalName,
                null));
        //when
        inputDestination.send(message);

        //then
        verify(runtimeService).signalEventReceived(signalName);
    }

    @Test
    public void shouldHaveChannelBindingsSetForSignalProducer() {
        //given
        String signalName = "signal";
        String destination = "signalEvent";

        //when
        signalPayloadEventListener.sendSignal(new SignalPayload(signalName,
                Map.of("test-variable", "value")));

        //then
        Message<byte[]> received = outputDestination.receive(0l, destination);
        assertNotNull(received);
    }
}