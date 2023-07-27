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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.api.process.runtime.events.ProcessCompletedEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.identity.IdentityService;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.AuditServiceInfoAppender;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
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

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender = new RuntimeBundleInfoAppender(
        new RuntimeBundleProperties()
    );

    private IdentityService identityService = mock(IdentityService.class);

    private AuditServiceInfoAppender auditServiceInfoAppender = spy(new AuditServiceInfoAppender(identityService));

    private ToCloudProcessRuntimeEventConverter eventConverter = spy(
        new ToCloudProcessRuntimeEventConverter(runtimeBundleInfoAppender, auditServiceInfoAppender)
    );

    private ProcessEngineEventsAggregator eventsAggregator = spy(
        new ProcessEngineEventsAggregator(mock(MessageProducerCommandContextCloseListener.class))
    );

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent> argumentCaptor;

    private static final String USERNAME = "myUserTest";

    private static final String USERNAME_GUID = "964b5dff-173a-4ba2-947d-1db16c1236a7";

    @BeforeEach
    void beforeEach() {
        User user = new User();
        user.setId(USERNAME_GUID);
        when(this.identityService.findUserByName(USERNAME)).thenReturn(user);
        when(this.eventsAggregator.getCurrentCommandContext()).thenReturn(this.commandContext);
    }

    @Test
    void should_setActorFromInitiator_when_invokeCloudProcessCompletedProducerOnEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setInitiator(USERNAME);
        ProcessCompletedEvent processCompletedEvent = new ProcessCompletedImpl(processInstance);
        CloudProcessCompletedProducer cloudProcessCompletedProducer = new CloudProcessCompletedProducer(
            this.eventConverter,
            this.eventsAggregator
        );

        cloudProcessCompletedProducer.onEvent(processCompletedEvent);

        verify(this.auditServiceInfoAppender).appendAuditServiceInfoTo(any(CloudRuntimeEventImpl.class), eq(USERNAME));
        verify(this.eventsAggregator).add(this.argumentCaptor.capture());
        assertThat(this.argumentCaptor.getValue().getActor()).isEqualTo(USERNAME_GUID);
    }
}
