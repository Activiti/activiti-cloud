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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationFailedEventImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationFailedBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationErrorSenderImplTest {

    @InjectMocks
    private IntegrationErrorSenderImpl integrationErrorSender;

    @Mock
    private IntegrationErrorChannelResolver resolver;

    @Mock
    AuditChannels auditChannels;

    @Mock
    ConnectorProperties properties;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private MessageChannel auditMessageChannel;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldSendMessageBasedOnTheTargetApplication() throws Exception {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");
        Throwable error = new Error("Boom!");
        IntegrationError IntegrationError = new IntegrationErrorImpl(integrationRequest,error);

        given(resolver.resolveDestination(integrationRequest)).willReturn(messageChannel);
        given(auditChannels.auditProducer()).willReturn(auditMessageChannel);

        Message<IntegrationError> message = MessageBuilder.withPayload(IntegrationError).build();

        //when
        integrationErrorSender.send(message);

        //then
        verify(messageChannel).send(message);
    }

    @Test
    public void shouldSendAuditMessageBasedOnTheIntegrationError() throws Exception {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");
        Throwable error = new Error("Boom!");
        IntegrationError IntegrationError = new IntegrationErrorImpl(integrationRequest,error);

        given(resolver.resolveDestination(integrationRequest)).willReturn(messageChannel);
        given(auditChannels.auditProducer()).willReturn(auditMessageChannel);

        Message<IntegrationError> message = MessageBuilder.withPayload(IntegrationError).build();
        Message<CloudRuntimeEvent<?, ?>[]> auditMessage = IntegrationFailedBuilder.failureFor(message.getPayload(), properties).buildMessage();

        //when
        integrationErrorSender.send(message);

        //then
        verify(auditMessageChannel).send(
            argThat(audit ->
                audit.getHeaders().get("integrationContextId").equals(auditMessage.getHeaders().get("integrationContextId")) &&
                    ((CloudRuntimeEvent<?, ?>[])audit.getPayload())[0].getClass().equals(CloudIntegrationFailedEventImpl.class) &&
                    ((CloudRuntimeEvent<?, ?>[])audit.getPayload())[0].getEntity().equals(((CloudRuntimeEvent<?, ?>[])auditMessage.getPayload())[0].getEntity())
                )
        );
    }
}
