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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.ExtendedCloudProcessRuntimeEvent.ExtendedCloudProcessRuntimeEvents;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessDeletedEventHandlerTest {

  @InjectMocks
  private ProcessDeletedEventHandler handler;

  @Mock
  private EntityManager entityManager;

  @Test
  public void handleShouldDeleteCurrentProcessInstance() {
    //given
    ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
    eventProcessInstance.setId(UUID.randomUUID().toString());
    CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(eventProcessInstance);

    ProcessInstanceEntity currentProcessInstanceEntity = buildEntity(eventProcessInstance.getId(), ProcessInstanceStatus.COMPLETED);
    given(entityManager.find(ProcessInstanceEntity.class, eventProcessInstance.getId())).willReturn(currentProcessInstanceEntity);

    //when
    handler.handle(event);

    //then
    verify(entityManager).remove(currentProcessInstanceEntity);
  }

  @Test
  public void handleShouldThrowExceptionWhenProcessInstanceIsNotCancelledOrCompleted() {
    //given
    ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
    eventProcessInstance.setId(UUID.randomUUID().toString());
    CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(eventProcessInstance);

    ProcessInstanceEntity entity = buildEntity(eventProcessInstance.getId(), ProcessInstanceStatus.RUNNING);
    given(entityManager.find(ProcessInstanceEntity.class, eventProcessInstance.getId())).willReturn(entity);

    //then
    //when
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> handler.handle(event))
        .withMessage(handler.INVALID_PROCESS_INSTANCE_STATE, eventProcessInstance.getId(), ProcessInstanceStatus.RUNNING.name());
  }

  @Test
  public void handleShouldThrowExceptionWhenRelatedProcessInstanceIsNotFound() {
    //given
    ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
    eventProcessInstance.setId(UUID.randomUUID().toString());
    CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(eventProcessInstance);

    given(entityManager.find(ProcessInstanceEntity.class, eventProcessInstance.getId())).willReturn(null);

    //then
    //when
    assertThatExceptionOfType(QueryException.class)
        .isThrownBy(() -> handler.handle(event))
        .withMessageContaining("Unable to find process instance with the given id: ");
  }

  @Test
  public void getHandledEventShouldReturnProcessDeletedEvent() {
    //when
    String handledEvent = handler.getHandledEvent();

    //then
    assertThat(handledEvent).isEqualTo(ExtendedCloudProcessRuntimeEvents.PROCESS_DELETED.name());
  }

  private ProcessInstanceEntity buildEntity(String id, ProcessInstanceStatus status) {
    ProcessInstanceEntity entity = new ProcessInstanceEntity();
    entity.setId(id);
    entity.setStatus(status);
    return entity;
  }
}
