/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.events.listeners;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CloudProcessDeployedProducerTest {

    @InjectMocks
    private CloudProcessDeployedProducer processDeployedProducer;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private ProcessEngineChannels producer;

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
        initMocks(this);
        when(producer.auditProducer()).thenReturn(auditProducer);
        when(runtimeBundleMessageBuilderFactory.create()).thenReturn(messageBuilderAppenderChain);
    }

    @Test
    public void shouldSendMessageWithDeployedProcessesWhenWebApplicationTypeIsServlet() {
        //given
        ProcessDefinition def1 = mock(ProcessDefinition.class);
        ProcessDefinition def2 = mock(ProcessDefinition.class);
        List<ProcessDeployedEvent> processDeployedEventList = Arrays.asList(new ProcessDeployedEventImpl(def1,
                                                                                                         "content1"),
                                                                            new ProcessDeployedEventImpl(def2,
                                                                                                         "content2"));
        given(messageBuilderAppenderChain.withPayload(any())).willReturn(MessageBuilder.withPayload(new CloudRuntimeEvent<?, ?>[2]));

        //when
        processDeployedProducer.sendProcessDeployedEvents(new ProcessDeployedEvents(processDeployedEventList));

        //then
        verify(runtimeBundleInfoAppender,
               times(2)).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(auditProducer).send(any());

        verify(messageBuilderAppenderChain).withPayload(messagePayloadCaptor.capture());
        List<CloudProcessDeployedEvent> cloudProcessDeployedEvents = Arrays.stream(messagePayloadCaptor.getValue())
                .map(CloudProcessDeployedEvent.class::cast)
                .collect(Collectors.toList());
        assertThat(cloudProcessDeployedEvents)
                .extracting(CloudProcessDeployedEvent::getEntity,
                            CloudProcessDeployedEvent::getProcessModelContent)
                .containsOnly(tuple(def1,
                                    "content1"),
                              tuple(def2,
                                    "content2"));
    }
}
