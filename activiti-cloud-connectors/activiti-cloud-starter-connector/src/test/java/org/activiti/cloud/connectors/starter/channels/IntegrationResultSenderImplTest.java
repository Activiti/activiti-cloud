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
package org.activiti.cloud.connectors.starter.channels;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
public class IntegrationResultSenderImplTest {

    @InjectMocks
    private IntegrationResultSenderImpl integrationResultSender;

    @Mock
    private IntegrationResultChannelResolver resolver;

    @Mock
    private MessageChannel messageChannel;

    @Test
    public void sendShouldSendMessageBasedOnTheTargetApplication() {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");
        IntegrationResult integrationResultEvent = new IntegrationResultImpl(
            integrationRequest,
            integrationRequest.getIntegrationContext()
        );

        given(resolver.resolveDestination(integrationRequest)).willReturn(messageChannel);

        Message<IntegrationResult> message = MessageBuilder.withPayload(integrationResultEvent).build();

        //when
        integrationResultSender.send(message);

        //then
        verify(messageChannel).send(message);
    }
}
