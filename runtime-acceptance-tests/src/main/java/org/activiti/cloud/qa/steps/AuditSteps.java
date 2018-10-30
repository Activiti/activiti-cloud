/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.steps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.thucydides.core.annotations.Step;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.AuditService;
import org.assertj.core.api.Condition;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * Audit steps
 */
@EnableRuntimeFeignContext
public class AuditSteps {

    @Autowired
    private AuditService auditService;

    @Step
    public void checkServicesHealth() {
        assertThat(auditService.isServiceUp()).isTrue();
    }

    @Step
    public Map<String, Object> health() {
        return auditService.health();
    }

    @Step
    public Collection<CloudRuntimeEvent> getEventsByProcessInstanceIdAndEventType(String processInstanceId,
                                                                                  String eventType) throws Exception {
        return auditService.getProcessInstanceEvents("eventType:" + eventType + ",processInstanceId:" + processInstanceId).getContent();
    }

    @Step
    public void checkProcessInstanceEvent(String processInstanceId,
                                          ProcessRuntimeEvent.ProcessEvents eventType) throws Exception {

        Collection<CloudRuntimeEvent> events = getEventsByProcessInstanceIdAndEventType(processInstanceId,
                                                                                        eventType.name());

        await().untilAsserted(() -> {

            assertThat(events).isNotEmpty();
            CloudRuntimeEvent resultingEvent = events.iterator().next();
            assertThat(resultingEvent).isNotNull();
            assertThat(resultingEvent).isInstanceOf(CloudProcessRuntimeEvent.class);
            assertThat(resultingEvent.getServiceName()).isNotEmpty();
            assertThat(resultingEvent.getServiceFullName()).isNotEmpty();

        });
    }

    @Step
    public void checkProcessInstanceTaskEvent(String processInstanceId,
                                              String taskId,
                                              TaskRuntimeEvent.TaskEvents eventType) throws Exception {

        Collection<CloudRuntimeEvent> events = getEventsByProcessInstanceIdAndEventType(processInstanceId,
                                                                            eventType.name());


        await().untilAsserted(() -> {

            assertThat(events).isNotEmpty();
            assertThat(events).extracting(e -> e.getEventType()).containsOnly(eventType);
            List<CloudRuntimeEvent> processInstanceTasks = events.stream().filter(e -> ((CloudTaskRuntimeEvent) e).getEntity().getProcessInstanceId().equals(processInstanceId)).collect(Collectors.toList());
            assertThat(processInstanceTasks).hasSize(1);
            CloudRuntimeEvent resultingEvent = processInstanceTasks.get(0);
            assertThat(resultingEvent).isNotNull();
            assertThat(resultingEvent).isInstanceOf(CloudTaskRuntimeEvent.class);
            assertThat(((CloudTaskRuntimeEvent) resultingEvent).getEntity().getId()).isEqualTo(taskId);
            assertThat(resultingEvent.getServiceName()).isNotEmpty();
            assertThat(resultingEvent.getServiceFullName()).isNotEmpty();

        });
    }


    @Step
    public void checkProcessInstanceVariableEvent(String processInstanceId,
                                              String variableName,
                                              VariableEvent.VariableEvents eventType) throws Exception {

        Collection<CloudRuntimeEvent> events = getEventsByProcessInstanceIdAndEventType(processInstanceId,
                eventType.name());


        await().untilAsserted(() -> {

            assertThat(events).isNotEmpty();
            assertThat(events).extracting(e -> e.getEventType()).containsOnly(eventType);
            List<CloudRuntimeEvent> processInstanceTasks = events.stream().filter(e -> ((CloudVariableEvent) e).getEntity().getProcessInstanceId().equals(processInstanceId)).collect(Collectors.toList());
            assertThat(processInstanceTasks).hasSize(1);
            CloudRuntimeEvent resultingEvent = processInstanceTasks.get(0);
            assertThat(resultingEvent).isNotNull();
            assertThat(resultingEvent).isInstanceOf(CloudVariableEvent.class);
            assertThat(((CloudVariableEvent) resultingEvent).getEntity().getName()).isEqualTo(variableName);
            assertThat(resultingEvent.getServiceName()).isNotEmpty();
            assertThat(resultingEvent.getServiceFullName()).isNotEmpty();

        });
    }
    @Step
    public Collection<CloudRuntimeEvent> getEvents() {
        return auditService.getEvents().getContent();
    }

    /**
     * Check if a standalone task was created
     * and assigned to it's creator
     * @param taskId the id of the task (from rb)
     */

    /**
     * Check if a task was created
     * @param taskId the id of the task (from rb)
     */
    public void checkTaskCreatedEvent(String taskId){

        final Collection<CloudRuntimeEvent> events = getEventsByEntityId(taskId);
        Condition<CloudRuntimeEvent> taskIsMatched = new Condition<CloudRuntimeEvent>() {
            @Override
            public boolean matches(CloudRuntimeEvent event) {

                return event instanceof CloudTaskRuntimeEvent && ((CloudTaskRuntimeEvent) event).getEntity() != null
                        && taskId.equals(((CloudTaskRuntimeEvent) event).getEntity().getId());
            }
        };

        await().untilAsserted(() -> assertThat(events).isNotNull()
                .isNotEmpty()
                .filteredOn(taskIsMatched).hasSize(1)
                .extracting("entity.id",
                        "entity.status",
                        "eventType")
                .containsExactly(
                        tuple(taskId,
                                Task.TaskStatus.CREATED,
                                TaskRuntimeEvent.TaskEvents.TASK_CREATED)));

    }

    @Step
    public void checkTaskCreatedAndAssignedEvents(String taskId) {

        final Collection<CloudRuntimeEvent> events = getEventsByEntityId(taskId);
        Condition<CloudRuntimeEvent> taskIsMatched = new Condition<CloudRuntimeEvent>() {
            @Override
            public boolean matches(CloudRuntimeEvent event) {

                return event instanceof CloudTaskRuntimeEvent && ((CloudTaskRuntimeEvent) event).getEntity() != null
                        && taskId.equals(((CloudTaskRuntimeEvent) event).getEntity().getId());
            }
        };

        await().untilAsserted(() -> assertThat(events).isNotNull()
                                    .isNotEmpty()
                                    .filteredOn(taskIsMatched).hasSize(2)
                                    .extracting("entity.id",
                                            "entity.status",
                                            "eventType")
                                    .containsExactly(
                                            tuple(taskId,
                                                  Task.TaskStatus.ASSIGNED,
                                                  TaskRuntimeEvent.TaskEvents.TASK_CREATED),
                                            tuple(taskId,
                                                  Task.TaskStatus.ASSIGNED,
                                                  TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED)));
    }

    /**
     * Check if a task was created
     * and assigned
     * and completed
     * @param taskId the id of the task (from rb)
     */
    @Step
    public void checkTaskCreatedAndAssignedAndCompletedEvents(String taskId) {

        final Collection<CloudRuntimeEvent> events = getEventsByEntityId(taskId);
        Condition<CloudRuntimeEvent> taskIsMatched = new Condition<CloudRuntimeEvent>() {
            @Override
            public boolean matches(CloudRuntimeEvent event) {

                return event instanceof CloudTaskRuntimeEvent && ((CloudTaskRuntimeEvent) event).getEntity() != null
                        && taskId.equals(((CloudTaskRuntimeEvent) event).getEntity().getId());
            }
        };

        await().untilAsserted(() -> assertThat(events).isNotNull()
                .isNotEmpty()
                .filteredOn(taskIsMatched).hasSize(3)
                .extracting("entity.id",
                        "entity.status",
                        "eventType")
                .containsExactly(
                        tuple(taskId,
                                Task.TaskStatus.COMPLETED,
                                TaskRuntimeEvent.TaskEvents.TASK_CREATED),
                        tuple(taskId,
                                Task.TaskStatus.COMPLETED,
                                TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED),
                        tuple(taskId,
                                Task.TaskStatus.COMPLETED,
                                TaskRuntimeEvent.TaskEvents.TASK_COMPLETED)));
    }

    /**
     * Check if a standalone task was cancelled.
     * @param taskId the id of the task
     */
    @Step
    public void checkTaskDeletedEvent(String taskId) {
        
        await().untilAsserted(() -> {
        assertThat(getEventsByEntityId(taskId))
                .isNotEmpty()
                .hasSize(3)
                .extracting("entityId",
                            "entity.id",
                            "entity.status",
                            "eventType")
                .containsExactly(
                        tuple(taskId,
                              taskId,
                              Task.TaskStatus.ASSIGNED,
                              TaskRuntimeEvent.TaskEvents.TASK_CREATED),
                        tuple(taskId,
                              taskId,
                              Task.TaskStatus.ASSIGNED,
                              TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED),
                        tuple(taskId,
                              taskId,
                              //TODO change to DELETED status and TASK_DELETED event when RB is ready
                              Task.TaskStatus.CANCELLED,
                              TaskRuntimeEvent.TaskEvents.TASK_CANCELLED));
        });
    }

    /**
     * Check if for a given task a new subtask is created
     * @param subtaskId the id of the task (from rb)
     * @param parentTaskId id of the parent task referenced in subtask
     */
    @Step
    public void checkSubtaskCreated(String subtaskId,
                                    String parentTaskId) {

        final Collection<CloudRuntimeEvent> events = getEventsByEntityId(subtaskId);

        await().untilAsserted(() -> assertThat(events).isNotNull()
                                    .isNotEmpty()
                                    .hasSize(2)
                                    .extracting("entityId", "entity.id",
                                                "entity.parentTaskId")
                                    .contains(tuple(subtaskId,
                                                    subtaskId,
                                                    parentTaskId),
                                              tuple(subtaskId,
                                                    subtaskId,
                                                    parentTaskId)));
    }

    @Step
    public Collection<CloudRuntimeEvent> getAllEvents(){
        return auditService.getEvents().getContent();
    }

    @Step
    public Collection<CloudRuntimeEvent> getEventsByEntityId(String entityId){
        String filter = "entityId:";
        return auditService.getEvents(filter + entityId).getContent();
    }

    @Step
    public Collection<CloudRuntimeEvent> getEventsByEntityIdAdmin(String entityId){
        String filter = "entityId:";
        return auditService.getEventsAdmin(filter + entityId).getContent();
    }
}
