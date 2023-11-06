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

package org.activiti.cloud.services.events.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.services.events.ActorConstants;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessAuditServiceInfoAppenderTest {

    private static final String EXPECTED_ACTOR = "actor1";

    @Mock
    private CommandContext commandContext;

    @Mock
    private ExecutionEntityManager executionEntityManager;

    @Mock
    private ExecutionEntity processInstance;

    private final String processInstanceId = UUID.randomUUID().toString();

    private ProcessAuditServiceInfoAppender processAuditServiceInfoAppender;

    private IdentityLinkEntityImpl identityLink;

    @BeforeEach
    void setUp() {
        this.processAuditServiceInfoAppender = new ProcessAuditServiceInfoAppender(() -> commandContext);
        this.identityLink = new IdentityLinkEntityImpl();

        when(this.commandContext.getExecutionEntityManager()).thenReturn(executionEntityManager);
        when(executionEntityManager.findById(eq(processInstanceId))).thenReturn(processInstance);
        when(processInstance.getIdentityLinks()).thenReturn(List.of(this.identityLink));
    }

    @Test
    void should_setAndGetActor_when_identityLinkTypeIsActor() {
        this.identityLink.setType(ActorConstants.ACTOR_TYPE);
        this.identityLink.setDetails(EXPECTED_ACTOR.getBytes());
        CloudProcessCompletedEventImpl processCompletedEvent = buildProcessCompletedEvent();

        CloudRuntimeEventImpl<ProcessInstance, ProcessEvents> processInstanceProcessEventsCloudRuntimeEvent =
            this.processAuditServiceInfoAppender.appendAuditServiceInfoTo(processCompletedEvent);

        assertThat(processInstanceProcessEventsCloudRuntimeEvent.getActor()).isEqualTo(EXPECTED_ACTOR);
    }

    @Test
    void should_setDefaultActor_when_IdentityLinkTypeIsNotActor() {
        CloudProcessCompletedEventImpl processCompletedEvent = buildProcessCompletedEvent();

        CloudRuntimeEventImpl<ProcessInstance, ProcessEvents> processInstanceProcessEventsCloudRuntimeEvent =
            this.processAuditServiceInfoAppender.appendAuditServiceInfoTo(processCompletedEvent);

        assertThat(processInstanceProcessEventsCloudRuntimeEvent.getActor()).isEqualTo("service_user");
    }

    private CloudProcessCompletedEventImpl buildProcessCompletedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(this.processInstanceId);
        return new CloudProcessCompletedEventImpl(processInstance);
    }
}
