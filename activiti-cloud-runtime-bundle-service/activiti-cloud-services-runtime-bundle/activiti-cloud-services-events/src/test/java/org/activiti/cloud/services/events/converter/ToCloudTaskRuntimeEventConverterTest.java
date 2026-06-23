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
import org.activiti.cloud.api.task.model.events.CloudTaskActivatedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCancelledEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupAddedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupRemovedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserAddedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserRemovedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCompletedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskSuspendedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskUpdatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.runtime.api.event.impl.TaskActivatedImpl;
import org.activiti.runtime.api.event.impl.TaskAssignedEventImpl;
import org.activiti.runtime.api.event.impl.TaskCancelledImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateGroupAddedEventImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateGroupRemovedImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateUserAddedEventImpl;
import org.activiti.runtime.api.event.impl.TaskCandidateUserRemovedImpl;
import org.activiti.runtime.api.event.impl.TaskCompletedImpl;
import org.activiti.runtime.api.event.impl.TaskCreatedEventImpl;
import org.activiti.runtime.api.event.impl.TaskSuspendedImpl;
import org.activiti.runtime.api.event.impl.TaskUpdatedEventImpl;
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
        assertThat(taskCompleted)
            .isInstanceOf(CloudTaskCompletedEvent.class)
            .returns("10", e -> e.getEntity().getId())
            .returns("myProcessDef", e -> e.getEntity().getProcessDefinitionId())
            .returns("myProcessDef", e -> e.getProcessDefinitionId())
            .returns(USERNAME_GUID, e -> e.getActor());

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
        assertThat(taskCreated)
            .isInstanceOf(CloudTaskCreatedEvent.class)
            .returns("10", e -> e.getEntity().getId())
            .returns("myProcessDef", e -> e.getEntity().getProcessDefinitionId())
            .returns("myProcessDef", e -> e.getProcessDefinitionId())
            .returns("service_user", e -> e.getActor());

        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(this.taskAuditServiceInfoAppender, never()).appendAuditServiceInfoTo(
            any(CloudTaskCompletedEventImpl.class)
        );
    }

    @Test
    void should_convertInternalTaskCandidateUserAddedEvent_when_convertToExternalEvent() {
        TaskCandidateUserImpl candidate = new TaskCandidateUserImpl(USERNAME, "task-1");
        TaskCandidateUserAddedEventImpl event = new TaskCandidateUserAddedEventImpl(candidate);
        event.setProcessInstanceId("proc-1");

        CloudTaskCandidateUserAddedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent)
            .isInstanceOf(CloudTaskCandidateUserAddedEvent.class)
            .returns("proc-1", CloudTaskCandidateUserAddedEvent::getProcessInstanceId)
            .returns(candidate, CloudTaskCandidateUserAddedEvent::getEntity)
            .returns(USERNAME, e -> e.getEntity().getUserId())
            .returns("task-1", e -> e.getEntity().getTaskId());
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskCandidateUserRemovedEvent_when_convertToExternalEvent() {
        TaskCandidateUserImpl candidate = new TaskCandidateUserImpl(USERNAME, "task-1");
        TaskCandidateUserRemovedImpl event = new TaskCandidateUserRemovedImpl(candidate);
        event.setProcessInstanceId("proc-2");

        CloudTaskCandidateUserRemovedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent)
            .isInstanceOf(CloudTaskCandidateUserRemovedEvent.class)
            .returns("proc-2", CloudTaskCandidateUserRemovedEvent::getProcessInstanceId)
            .returns(candidate, CloudTaskCandidateUserRemovedEvent::getEntity)
            .returns(USERNAME, e -> e.getEntity().getUserId())
            .returns("task-1", e -> e.getEntity().getTaskId());
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskCandidateGroupAddedEvent_when_convertToExternalEvent() {
        TaskCandidateGroupImpl candidate = new TaskCandidateGroupImpl("group-1", "task-1");
        TaskCandidateGroupAddedEventImpl event = new TaskCandidateGroupAddedEventImpl(candidate);
        event.setProcessInstanceId("proc-3");

        CloudTaskCandidateGroupAddedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent)
            .isInstanceOf(CloudTaskCandidateGroupAddedEvent.class)
            .returns("proc-3", CloudTaskCandidateGroupAddedEvent::getProcessInstanceId)
            .returns(candidate, CloudTaskCandidateGroupAddedEvent::getEntity)
            .returns("group-1", e -> e.getEntity().getGroupId())
            .returns("task-1", e -> e.getEntity().getTaskId());
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskCandidateGroupRemovedEvent_when_convertToExternalEvent() {
        TaskCandidateGroupImpl candidate = new TaskCandidateGroupImpl("group-1", "task-1");
        TaskCandidateGroupRemovedImpl event = new TaskCandidateGroupRemovedImpl(candidate);
        event.setProcessInstanceId("proc-4");

        CloudTaskCandidateGroupRemovedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent)
            .isInstanceOf(CloudTaskCandidateGroupRemovedEvent.class)
            .returns("proc-4", CloudTaskCandidateGroupRemovedEvent::getProcessInstanceId)
            .returns(candidate, CloudTaskCandidateGroupRemovedEvent::getEntity)
            .returns("group-1", e -> e.getEntity().getGroupId())
            .returns("task-1", e -> e.getEntity().getTaskId());
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskAssignedEvent_when_convertToExternalEvent() {
        TaskImpl task = new TaskImpl();
        task.setId("11");
        task.setProcessDefinitionId("myProcessDef");
        TaskAssignedEventImpl event = new TaskAssignedEventImpl(task);

        CloudTaskAssignedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent)
            .returns("11", e -> e.getEntity().getId())
            .returns("myProcessDef", CloudTaskAssignedEvent::getProcessDefinitionId);
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskUpdatedEvent_when_convertToExternalEvent() {
        TaskImpl task = new TaskImpl();
        task.setId("12");
        task.setProcessDefinitionId("myProcessDef");
        TaskUpdatedEventImpl event = new TaskUpdatedEventImpl(task);

        CloudTaskUpdatedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent)
            .returns("12", e -> e.getEntity().getId())
            .returns("myProcessDef", CloudTaskUpdatedEvent::getProcessDefinitionId);
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskCancelledEvent_when_convertToExternalEvent() {
        TaskImpl task = new TaskImpl();
        task.setId("13");
        TaskCancelledImpl event = new TaskCancelledImpl(task, "user request");

        CloudTaskCancelledEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getEntity().getId()).isEqualTo("13");
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskSuspendedEvent_when_convertToExternalEvent() {
        TaskImpl task = new TaskImpl();
        task.setId("14");
        TaskSuspendedImpl event = new TaskSuspendedImpl(task);

        CloudTaskSuspendedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getEntity().getId()).isEqualTo("14");
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalTaskActivatedEvent_when_convertToExternalEvent() {
        TaskImpl task = new TaskImpl();
        task.setId("15");
        TaskActivatedImpl event = new TaskActivatedImpl(task);

        CloudTaskActivatedEvent cloudEvent = this.converter.from(event);

        assertThat(cloudEvent.getEntity().getId()).isEqualTo("15");
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }
}
