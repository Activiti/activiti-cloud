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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.api.runtime.event.impl.ProcessDeployedEventImpl;
import org.activiti.api.runtime.event.impl.ProcessDeployedEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties.RuntimeBundleEventsProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderAppenderChain;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
public class CloudProcessDeployedProducerTest {

    private CloudProcessDeployedProducer processDeployedProducer;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private ProcessEngineChannels producer;

    @Spy
    private RuntimeBundleProperties properties = new RuntimeBundleProperties();

    @Mock
    private MessageChannel auditProducer;

    @Mock
    private RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;

    @Mock
    private MessageBuilderAppenderChain messageBuilderAppenderChain;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent<?, ?>[]> messagePayloadCaptor;

    @BeforeEach
    public void setUp() {
        when(producer.auditProducer()).thenReturn(auditProducer);
        when(runtimeBundleMessageBuilderFactory.create()).thenReturn(messageBuilderAppenderChain);
        final RuntimeBundleEventsProperties eventsProperties = new RuntimeBundleEventsProperties();
        eventsProperties.setChunkSize(2);
        properties.setEventsProperties(eventsProperties);
        processDeployedProducer =
            new CloudProcessDeployedProducer(
                runtimeBundleInfoAppender,
                producer,
                runtimeBundleMessageBuilderFactory,
                properties
            );
    }

    @Test
    public void should_sendMessageWithDeployedProcessesInChunks() {
        //given
        ProcessDefinition def1 = mock(ProcessDefinition.class);
        ProcessDefinition def2 = mock(ProcessDefinition.class);
        ProcessDefinition def3 = mock(ProcessDefinition.class);
        List<ProcessDeployedEvent> processDeployedEventList = Arrays.asList(
            new ProcessDeployedEventImpl(def1, "content1"),
            new ProcessDeployedEventImpl(def2, "content2"),
            new ProcessDeployedEventImpl(def3, "content3")
        );
        given(messageBuilderAppenderChain.withPayload(any()))
            .willReturn(MessageBuilder.withPayload(new CloudRuntimeEvent<?, ?>[2]));

        //when
        processDeployedProducer.sendProcessDeployedEvents(new ProcessDeployedEvents(processDeployedEventList));

        //then
        verify(runtimeBundleInfoAppender, times(3)).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(messageBuilderAppenderChain, times(2)).withPayload(messagePayloadCaptor.capture());
        verify(auditProducer, times(2)).send(any());

        List<CloudRuntimeEvent<?, ?>[]> values = messagePayloadCaptor.getAllValues();
        List<CloudProcessDeployedEvent> cloudProcessDeployedEvents = Arrays
            .stream(values.get(0))
            .map(CloudProcessDeployedEvent.class::cast)
            .collect(Collectors.toList());
        assertThat(cloudProcessDeployedEvents)
            .extracting(CloudProcessDeployedEvent::getEntity, CloudProcessDeployedEvent::getProcessModelContent)
            .containsOnly(tuple(def1, "content1"), tuple(def2, "content2"));

        assertThat(Arrays.stream(values.get(1)).map(CloudProcessDeployedEvent.class::cast).collect(Collectors.toList()))
            .extracting(CloudProcessDeployedEvent::getEntity, CloudProcessDeployedEvent::getProcessModelContent)
            .containsOnly(tuple(def3, "content3"));
    }
}
