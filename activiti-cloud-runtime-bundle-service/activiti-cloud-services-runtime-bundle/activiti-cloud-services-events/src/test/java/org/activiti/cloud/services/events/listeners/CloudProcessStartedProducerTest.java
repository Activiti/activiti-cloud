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

import java.util.List;
import org.activiti.api.process.runtime.events.ProcessStartedEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.events.ActorConstants;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.ProcessAuditServiceInfoAppender;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityImpl;
import org.activiti.runtime.api.event.impl.ProcessStartedEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloudProcessStartedProducerTest {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender = new RuntimeBundleInfoAppender(
        new RuntimeBundleProperties()
    );

    private RuntimeService runtimeService = mock(RuntimeService.class);

    private ProcessAuditServiceInfoAppender processAuditServiceInfoAppender = spy(
        new ProcessAuditServiceInfoAppender(runtimeService)
    );

    private ToCloudProcessRuntimeEventConverter eventConverter = spy(
        new ToCloudProcessRuntimeEventConverter(runtimeBundleInfoAppender, processAuditServiceInfoAppender)
    );

    private ProcessEngineEventsAggregator eventsAggregator = spy(
        new ProcessEngineEventsAggregator(mock(MessageProducerCommandContextCloseListener.class))
    );

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent> argumentCaptor;

    @Captor
    private ArgumentCaptor<CloudProcessStartedEventImpl> argumentCaptorConverter;

    @BeforeEach
    void beforeEach() {
        when(this.eventsAggregator.getCurrentCommandContext()).thenReturn(this.commandContext);
    }

    @Test
    void should_setDefaultActor_when_invokeCloudProcessStartedProducerOnEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        String initiator = "myUserTest";
        processInstance.setInitiator(initiator);
        ProcessStartedEvent processCompletedEvent = new ProcessStartedEventImpl(processInstance);
        CloudProcessStartedProducer cloudProcessStartedProducer = new CloudProcessStartedProducer(
            this.eventConverter,
            this.eventsAggregator
        );

        cloudProcessStartedProducer.onEvent(processCompletedEvent);

        verify(this.processAuditServiceInfoAppender).appendAuditServiceInfoTo(this.argumentCaptorConverter.capture());
        assertThat(this.argumentCaptorConverter.getValue().getActor()).isEqualTo("service_user");
        verify(this.eventsAggregator).add(this.argumentCaptor.capture());
        assertThat(this.argumentCaptor.getValue().getActor()).isEqualTo("service_user");
    }

    @Test
    void should_setActor_when_invokeCloudProcessStartedProducerOnEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        String initiator = "myUserTest";
        processInstance.setInitiator(initiator);
        ProcessStartedEvent processCompletedEvent = new ProcessStartedEventImpl(processInstance);
        CloudProcessStartedProducer cloudProcessStartedProducer = new CloudProcessStartedProducer(
            this.eventConverter,
            this.eventsAggregator
        );
        final String expectedActor = "myActor";
        mockIdentityLinkActor(expectedActor, processInstance);

        cloudProcessStartedProducer.onEvent(processCompletedEvent);

        verify(this.processAuditServiceInfoAppender).appendAuditServiceInfoTo(this.argumentCaptorConverter.capture());
        assertThat(this.argumentCaptorConverter.getValue().getActor()).isEqualTo(expectedActor);
        verify(this.eventsAggregator).add(this.argumentCaptor.capture());
        assertThat(this.argumentCaptor.getValue().getActor()).isEqualTo(expectedActor);
    }

    private void mockIdentityLinkActor(String expectedActor, ProcessInstanceImpl processInstance) {
        IdentityLinkEntityImpl identityLinkEntity = new IdentityLinkEntityImpl();
        identityLinkEntity.setType(ActorConstants.ACTOR_TYPE);
        identityLinkEntity.setDetails(expectedActor.getBytes());
        when(this.runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()))
            .thenReturn(List.of(identityLinkEntity));
    }
}
