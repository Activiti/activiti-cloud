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
package org.activiti.cloud.services.events.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.Deployment;
import org.activiti.api.process.model.events.ApplicationDeployedEvent;
import org.activiti.api.runtime.event.impl.ApplicationDeployedEventImpl;
import org.activiti.api.runtime.event.impl.ApplicationDeployedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudApplicationDeployedEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderAppenderChain;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CloudApplicationDeployedProducerTest {

    @InjectMocks private CloudApplicationDeployedProducer cloudApplicationDeployedProducer;

    @Mock private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock private ProcessEngineChannels producer;

    @Mock private MessageChannel auditProducer;

    @Mock private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;

    @Mock private MessageBuilderAppenderChain messageBuilderAppenderChain;

    @Captor private ArgumentCaptor<CloudRuntimeEvent<?, ?>[]> messagePayloadCaptor;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(producer.auditProducer()).thenReturn(auditProducer);
        when(runtimeBundleMessageBuilderFactory.create()).thenReturn(messageBuilderAppenderChain);
    }

    @Test
    public void shouldSendMessageWithDeployedApplication() {
        // given
        Deployment deployment1 = mock(Deployment.class);
        Deployment deployment2 = mock(Deployment.class);
        List<ApplicationDeployedEvent> applicationDeployedEventList =
                Arrays.asList(
                        new ApplicationDeployedEventImpl(deployment1),
                        new ApplicationDeployedEventImpl(deployment2));
        given(messageBuilderAppenderChain.withPayload(any()))
                .willReturn(MessageBuilder.withPayload(new CloudRuntimeEvent<?, ?>[2]));

        // when
        cloudApplicationDeployedProducer.sendApplicationDeployedEvents(
                new ApplicationDeployedEvents(applicationDeployedEventList));

        // then
        verify(runtimeBundleInfoAppender, times(2))
                .appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(auditProducer).send(any());

        verify(messageBuilderAppenderChain).withPayload(messagePayloadCaptor.capture());
        List<CloudApplicationDeployedEvent> cloudApplicationDeployedEvents =
                Arrays.stream(messagePayloadCaptor.getValue())
                        .map(CloudApplicationDeployedEvent.class::cast)
                        .collect(Collectors.toList());
        assertThat(cloudApplicationDeployedEvents)
                .extracting(CloudApplicationDeployedEvent::getEntity)
                .containsOnly(deployment1, deployment2);
    }
}
