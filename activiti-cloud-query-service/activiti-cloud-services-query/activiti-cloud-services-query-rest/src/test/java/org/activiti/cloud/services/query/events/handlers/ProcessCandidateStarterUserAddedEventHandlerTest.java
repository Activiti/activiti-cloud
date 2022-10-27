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

import org.activiti.api.process.model.events.ProcessCandidateStarterUserEvent;
import org.activiti.api.runtime.model.impl.ProcessCandidateStarterUserImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterUserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProcessCandidateStarterUserAddedEventHandlerTest {

    @InjectMocks
    private ProcessCandidateStarterUserAddedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldStoreNewProcessCandidateStarterUser() {
        //given
        ProcessCandidateStarterUserImpl candidateUser = new ProcessCandidateStarterUserImpl(UUID.randomUUID().toString(),
                                                                        UUID.randomUUID().toString());
        CloudProcessCandidateStarterUserAddedEventImpl event = new CloudProcessCandidateStarterUserAddedEventImpl(candidateUser);

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<ProcessCandidateStarterUserEntity> captor = ArgumentCaptor.forClass(ProcessCandidateStarterUserEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue().getProcessDefinitionId()).isEqualTo(event.getEntity().getProcessDefinitionId());
        assertThat(captor.getValue().getUserId()).isEqualTo(event.getEntity().getUserId());
    }

    @Test
    public void getHandledEventShouldReturnProcessCandidateStarterUserAddedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents.PROCESS_CANDIDATE_STARTER_USER_ADDED.name());
    }

}
