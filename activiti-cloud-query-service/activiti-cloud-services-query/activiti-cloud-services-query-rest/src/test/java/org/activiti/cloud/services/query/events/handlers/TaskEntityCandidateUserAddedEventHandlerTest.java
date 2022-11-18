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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskEntityCandidateUserAddedEventHandlerTest {

    @InjectMocks
    private TaskCandidateUserAddedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldStoreNewTaskCandidateUser() {
        //given
        TaskCandidateUserImpl candidateUser = new TaskCandidateUserImpl(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        CloudTaskCandidateUserAddedEventImpl event = new CloudTaskCandidateUserAddedEventImpl(candidateUser);

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<TaskCandidateUserEntity> captor = ArgumentCaptor.forClass(TaskCandidateUserEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(captor.getValue().getUserId()).isEqualTo(event.getEntity().getUserId());
    }

    @Test
    public void getHandledEventShouldReturnTaskCandidateUserEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent)
            .isEqualTo(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED.name());
    }
}
