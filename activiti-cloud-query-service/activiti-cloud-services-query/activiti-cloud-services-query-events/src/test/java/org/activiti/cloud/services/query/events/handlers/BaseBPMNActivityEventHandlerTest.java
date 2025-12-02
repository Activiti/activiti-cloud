/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import static org.activiti.cloud.services.query.events.handlers.BaseBPMNActivityEventHandler.SERVICE_TASK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.Optional;
import org.activiti.cloud.api.process.model.impl.CloudBPMNActivityImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseBPMNActivityEventHandlerTest {

    @Mock
    private EntityManager entityManager;

    @Test
    void findOrCreateBPMNActivityEntity_shouldNotCreateEntityForServiceTask() {
        // given
        TestBaseBPMNActivityEventHandler handler = new TestBaseBPMNActivityEventHandler(entityManager);

        CloudBPMNActivityImpl serviceTaskActivity = createBPMNActivity(SERVICE_TASK_TYPE);
        CloudBPMNActivityStartedEventImpl event = createEvent(serviceTaskActivity);

        // when
        Optional<BaseBPMNActivityEntity> result = handler.findOrCreateBPMNActivityEntity(event);

        // then
        assertThat(result).isEmpty();
        verify(entityManager, never()).find(any(), any());
    }

    @Test
    void findOrCreateBPMNActivityEntity_shouldCreateEntityForUserTask() {
        // given
        TestBaseBPMNActivityEventHandler handler = new TestBaseBPMNActivityEventHandler(entityManager);

        CloudBPMNActivityImpl userTaskActivity = createBPMNActivity("userTask");
        CloudBPMNActivityStartedEventImpl event = createEvent(userTaskActivity);

        when(entityManager.find(eq(BPMNActivityEntity.class), any(String.class))).thenReturn(null);

        // when
        Optional<BaseBPMNActivityEntity> result = handler.findOrCreateBPMNActivityEntity(event);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getActivityType()).isEqualTo("userTask");
        assertThat(result.get().getElementId()).isEqualTo("element-123");
        assertThat(result.get().getProcessInstanceId()).isEqualTo("process-instance-123");
    }

    @Test
    void findOrCreateBPMNActivityEntity_shouldFindExistingEntityForUserTask() {
        // given
        TestBaseBPMNActivityEventHandler handler = new TestBaseBPMNActivityEventHandler(entityManager);

        CloudBPMNActivityImpl userTaskActivity = createBPMNActivity("userTask");
        CloudBPMNActivityStartedEventImpl event = createEvent(userTaskActivity);

        BPMNActivityEntity existingEntity = new BPMNActivityEntity(
            "serviceName",
            "serviceFullName",
            "serviceVersion",
            "appName",
            "appVersion"
        );
        existingEntity.setId("existing-id");
        existingEntity.setActivityType("userTask");

        when(entityManager.find(eq(BPMNActivityEntity.class), any(String.class))).thenReturn(existingEntity);

        // when
        Optional<BaseBPMNActivityEntity> result = handler.findOrCreateBPMNActivityEntity(event);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(existingEntity);
        assertThat(result.get().getId()).isEqualTo("existing-id");
    }

    @Test
    void createBpmnActivityEntity_shouldNotCreateEntityForServiceTask() {
        // given
        TestBaseBPMNActivityEventHandler handler = new TestBaseBPMNActivityEventHandler(entityManager);

        CloudBPMNActivityImpl serviceTaskActivity = createBPMNActivity(SERVICE_TASK_TYPE);
        CloudBPMNActivityStartedEventImpl event = createEvent(serviceTaskActivity);

        // when
        Optional<BaseBPMNActivityEntity> result = handler.createBpmnActivityEntity(event);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void createBpmnActivityEntity_shouldCreateEntityForCallActivity() {
        // given
        TestBaseBPMNActivityEventHandler handler = new TestBaseBPMNActivityEventHandler(entityManager);

        CloudBPMNActivityImpl callActivity = createBPMNActivity("callActivity");
        CloudBPMNActivityStartedEventImpl event = createEvent(callActivity);

        // when
        Optional<BaseBPMNActivityEntity> result = handler.createBpmnActivityEntity(event);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getActivityType()).isEqualTo("callActivity");
        assertThat(result.get().getActivityName()).isEqualTo("Test Activity");
        assertThat(result.get().getElementId()).isEqualTo("element-123");
        assertThat(result.get().getProcessDefinitionId()).isEqualTo("process-def-123");
        assertThat(result.get().getProcessInstanceId()).isEqualTo("process-instance-123");
        assertThat(result.get().getExecutionId()).isEqualTo("execution-123");
        assertThat(result.get().getProcessDefinitionKey()).isEqualTo("process-key");
        assertThat(result.get().getProcessDefinitionVersion()).isEqualTo(1);
        assertThat(result.get().getBusinessKey()).isEqualTo("business-key-123");
    }

    @Test
    public void createBpmnActivityEntity_shouldCreateEntityForScriptTask() {
        // given
        TestBaseBPMNActivityEventHandler handler = new TestBaseBPMNActivityEventHandler(entityManager);

        CloudBPMNActivityImpl scriptTask = createBPMNActivity("scriptTask");
        CloudBPMNActivityStartedEventImpl event = createEvent(scriptTask);

        // when
        Optional<BaseBPMNActivityEntity> result = handler.createBpmnActivityEntity(event);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getActivityType()).isEqualTo("scriptTask");
    }

    private CloudBPMNActivityImpl createBPMNActivity(String activityType) {
        CloudBPMNActivityImpl activity = new CloudBPMNActivityImpl();
        activity.setId("activity-123");
        activity.setActivityName("Test Activity");
        activity.setActivityType(activityType);
        activity.setElementId("element-123");
        activity.setProcessDefinitionId("process-def-123");
        activity.setProcessInstanceId("process-instance-123");
        activity.setExecutionId("execution-123");
        return activity;
    }

    private CloudBPMNActivityStartedEventImpl createEvent(CloudBPMNActivityImpl activity) {
        CloudBPMNActivityStartedEventImpl event = new CloudBPMNActivityStartedEventImpl(
            "event-123",
            new Date().getTime(),
            activity,
            "process-def-123",
            "process-instance-123"
        );
        event.setServiceName("serviceName");
        event.setServiceFullName("serviceFullName");
        event.setServiceVersion("serviceVersion");
        event.setAppName("appName");
        event.setAppVersion("appVersion");
        event.setProcessDefinitionKey("process-key");
        event.setProcessDefinitionVersion(1);
        event.setBusinessKey("business-key-123");
        return event;
    }

    // Test implementation to expose protected methods
    private static class TestBaseBPMNActivityEventHandler extends BaseBPMNActivityEventHandler {

        public TestBaseBPMNActivityEventHandler(EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        public Optional<BaseBPMNActivityEntity> findOrCreateBPMNActivityEntity(
            org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent<?, ?> event
        ) {
            return super.findOrCreateBPMNActivityEntity(event);
        }

        @Override
        public Optional<BaseBPMNActivityEntity> createBpmnActivityEntity(
            org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent<?, ?> event
        ) {
            return super.createBpmnActivityEntity(event);
        }
    }
}
