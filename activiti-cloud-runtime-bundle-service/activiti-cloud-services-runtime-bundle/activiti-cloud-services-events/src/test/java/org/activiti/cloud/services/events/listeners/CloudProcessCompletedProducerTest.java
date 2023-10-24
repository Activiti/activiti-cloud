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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.constraints.NotNull;
import org.activiti.api.process.runtime.events.ProcessCompletedEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.runtime.api.event.impl.ProcessCompletedImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloudProcessCompletedProducerTest {

    private static final String USERNAME = "myUserTest";

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender = new RuntimeBundleInfoAppender(
        new RuntimeBundleProperties()
    );

    private RuntimeService runtimeService = mock(RuntimeService.class);

    private ToCloudProcessRuntimeEventConverter eventConverter = spy(
        new ToCloudProcessRuntimeEventConverter(runtimeBundleInfoAppender)
    );

    private ProcessEngineEventsAggregator eventsAggregator = spy(
        new ProcessEngineEventsAggregator(mock(MessageProducerCommandContextCloseListener.class))
    );

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent> argumentCaptor;

    private CloudProcessCompletedProducer cloudProcessCompletedProducer;

    @BeforeEach
    void beforeEach() {
        when(this.eventsAggregator.getCurrentCommandContext()).thenReturn(this.commandContext);
        this.cloudProcessCompletedProducer =
            new CloudProcessCompletedProducer(this.eventConverter, this.eventsAggregator);
    }

    @Test
    void should_setDefaultActor_when_invokeCloudProcessCompletedProducerOnEvent() {
        ProcessCompletedEvent processCompletedEvent = buildProcessCompletedEvent();

        this.cloudProcessCompletedProducer.onEvent(processCompletedEvent);

        verify(this.eventsAggregator).add(this.argumentCaptor.capture());
        assertThat(this.argumentCaptor.getValue().getActor()).isEqualTo("service_user");
    }

    @NotNull
    private ProcessCompletedEvent buildProcessCompletedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setInitiator(USERNAME);
        ProcessCompletedEvent processCompletedEvent = new ProcessCompletedImpl(processInstance);
        return processCompletedEvent;
    }
}
