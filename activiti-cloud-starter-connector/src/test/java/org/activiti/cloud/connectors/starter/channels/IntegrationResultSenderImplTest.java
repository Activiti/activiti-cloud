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

import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.runtime.api.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrationResultSenderImplTest {

    @InjectMocks
    private IntegrationResultSenderImpl integrationResultSender;

    @Mock
    private BinderAwareChannelResolver resolver;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private ConnectorProperties connectorProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void sendShouldSendMessageBasedOnTheTargetApplication() throws Exception {
        //given
        given(resolver.resolveDestination("integrationResult:myApp")).willReturn(messageChannel);
        given(connectorProperties.getServiceName()).willReturn("connectorName");

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");
        IntegrationResult integrationResultEvent = new IntegrationResultImpl(integrationRequest,
                                                                             integrationRequest.getIntegrationContext());

        Message<IntegrationResult> message = MessageBuilder.withPayload(integrationResultEvent).build();

        //when
        integrationResultSender.send(message);

        //then
        verify(messageChannel).send(message);
    }
}