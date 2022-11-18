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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.MessageChannel;

@ExtendWith(MockitoExtension.class)
public class IntegrationResultChannelResolverImplTest {

    private IntegrationResultChannelResolver subject;

    @Mock
    private BinderAwareChannelResolver resolver;

    private IntegrationResultDestinationBuilder builder;

    @Mock
    private ConnectorProperties connectorProperties;

    @Mock
    private MessageChannel messageChannel;

    @BeforeEach
    public void setUp() {
        when(connectorProperties.getMqDestinationSeparator()).thenReturn(".");
        when(resolver.resolveDestination(anyString())).thenReturn(messageChannel);

        builder = spy(new IntegrationResultDestinationBuilderImpl(connectorProperties));

        subject = new IntegrationResultChannelResolverImpl(resolver, builder);
    }

    @Test
    public void shouldResolveDestination() {
        // given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");

        // when
        MessageChannel resut = subject.resolveDestination(integrationRequest);

        // then
        assertThat(resut).isEqualTo(messageChannel);

        verify(builder).buildDestination(integrationRequest);
    }
}
