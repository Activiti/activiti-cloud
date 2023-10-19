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
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskAuditServiceInfoAppenderTest {

    private static final String USERNAME_GUID = "964b5dff-173a-4ba2-947d-1db16c1236a7";

    @Mock
    private SecurityContextPrincipalProvider securityContextPrincipalProvider;

    @Mock
    private Principal principal;

    private TaskAuditServiceInfoAppender taskAuditServiceInfoAppender;

    @BeforeEach
    void setUp() {
        this.taskAuditServiceInfoAppender = new TaskAuditServiceInfoAppender(this.securityContextPrincipalProvider);
        when(this.securityContextPrincipalProvider.getCurrentPrincipal()).thenReturn(Optional.of(this.principal));
    }

    @Test
    void should_setAndGetActor_when_userHasNameSet() {
        when(this.principal.getName()).thenReturn(USERNAME_GUID);
        CloudTaskCompletedEventImpl taskCompletedEvent = getTaskCompletedEvent();

        CloudRuntimeEventImpl<Task, TaskEvents> taskTaskEventsCloudRuntimeEvent =
            this.taskAuditServiceInfoAppender.appendAuditServiceInfoTo(taskCompletedEvent);

        assertThat(taskTaskEventsCloudRuntimeEvent.getActor()).isEqualTo(USERNAME_GUID);
    }

    @Test
    void should_setDefaultActor_when_userHasNoNameSet() {
        CloudTaskCompletedEventImpl taskCompletedEvent = getTaskCompletedEvent();

        CloudRuntimeEventImpl<Task, TaskEvents> taskTaskEventsCloudRuntimeEvent =
            this.taskAuditServiceInfoAppender.appendAuditServiceInfoTo(taskCompletedEvent);

        assertThat(taskTaskEventsCloudRuntimeEvent.getActor()).isEqualTo("service_user");
    }

    private CloudTaskCompletedEventImpl getTaskCompletedEvent() {
        return new CloudTaskCompletedEventImpl(
            new TaskImpl(UUID.randomUUID().toString(), "my task", Task.TaskStatus.COMPLETED)
        );
    }
}
