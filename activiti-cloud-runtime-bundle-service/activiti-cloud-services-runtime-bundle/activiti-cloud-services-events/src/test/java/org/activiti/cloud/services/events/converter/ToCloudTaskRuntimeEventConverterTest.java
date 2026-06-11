/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.api.task.runtime.events.TaskCreatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupAddedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupRemovedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserAddedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserRemovedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCompletedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateGroupAddedEventImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateGroupRemovedImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateUserAddedEventImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateUserRemovedImpl;
import org.activiti.runtime.api.event.impl.TaskCompletedImpl;
import org.activiti.runtime.api.event.impl.TaskCreatedEventImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToCloudTaskRuntimeEventConverterTest {

    @InjectMocks
    private ToCloudTaskRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    private final SecurityContextPrincipalProvider securityContextPrincipalProvider = mock(
        SecurityContextPrincipalProvider.class
    );

    @Spy
    private TaskAuditServiceInfoAppender taskAuditServiceInfoAppender = new TaskAuditServiceInfoAppender(
        securityContextPrincipalProvider
    );

    private static final String USERNAME = "user1";

    private static final String USERNAME_GUID = "964b5dff-173a-4ba2-947d-1db16c1236a7";

    @Test
    void should_convertInternalTaskCompletedEvent_when_convertToExternalEvent() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(USERNAME_GUID);
        when(this.securityContextPrincipalProvider.getCurrentPrincipal()).thenReturn(Optional.of(principal));

        //given
        TaskImpl task = new TaskImpl();
        task.setId("10");
        task.setProcessDefinitionId("myProcessDef");
        task.setCompletedBy(USERNAME);

        TaskCompletedImpl event = new TaskCompletedImpl(task);

        //when
        CloudTaskCompletedEvent taskCompleted = this.converter.from(event);

        //then
        assertThat(taskCompleted).isInstanceOf(CloudTaskCompletedEvent.class);

        assertThat(taskCompleted.getEntity().getId()).isEqualTo("10");
        assertThat(taskCompleted.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(taskCompleted.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(taskCompleted.getActor()).isEqualTo(USERNAME_GUID);

        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(this.taskAuditServiceInfoAppender).appendAuditServiceInfoTo(any(CloudTaskCompletedEventImpl.class));
    }

    @Test
    void should_convertInternalTaskCreatedEvent_when_convertToExternalEvent() {
        //given
        TaskImpl task = new TaskImpl();
        task.setId("10");
        task.setProcessDefinitionId("myProcessDef");
        task.setCompletedBy(USERNAME);

        TaskCreatedEvent event = new TaskCreatedEventImpl(task);

        //when
        CloudTaskCreatedEvent taskCreated = this.converter.from(event);

        //then
        assertThat(taskCreated).isInstanceOf(CloudTaskCreatedEvent.class);

        assertThat(taskCreated.getEntity().getId()).isEqualTo("10");
        assertThat(taskCreated.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(taskCreated.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(taskCreated.getActor()).isEqualTo("service_user");

        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(this.taskAuditServiceInfoAppender, never())
            .appendAuditServiceInfoTo(any(CloudTaskCompletedEventImpl.class));
    }

    @Test
    void should_setProcessInstanceId_when_convertingTaskCandidateUserAddedEvent() {
        TaskCandidateUserAddedEventImpl event = new TaskCandidateUserAddedEventImpl(
            new TaskCandidateUserImpl(USERNAME, "task-1")
        );
        event.setProcessInstanceId("proc-1");

        CloudTaskCandidateUserAddedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("proc-1");
    }

    @Test
    void should_setProcessInstanceId_when_convertingTaskCandidateUserRemovedEvent() {
        TaskCandidateUserRemovedImpl event = new TaskCandidateUserRemovedImpl(
            new TaskCandidateUserImpl(USERNAME, "task-1")
        );
        event.setProcessInstanceId("proc-2");

        CloudTaskCandidateUserRemovedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("proc-2");
    }

    @Test
    void should_setProcessInstanceId_when_convertingTaskCandidateGroupAddedEvent() {
        TaskCandidateGroupAddedEventImpl event = new TaskCandidateGroupAddedEventImpl(
            new TaskCandidateGroupImpl("group-1", "task-1")
        );
        event.setProcessInstanceId("proc-3");

        CloudTaskCandidateGroupAddedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("proc-3");
    }

    @Test
    void should_setProcessInstanceId_when_convertingTaskCandidateGroupRemovedEvent() {
        TaskCandidateGroupRemovedImpl event = new TaskCandidateGroupRemovedImpl(
            new TaskCandidateGroupImpl("group-1", "task-1")
        );
        event.setProcessInstanceId("proc-4");

        CloudTaskCandidateGroupRemovedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("proc-4");
    }
}
