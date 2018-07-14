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

package org.activiti.cloud.starter.audit.tests.it;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.runtime.api.event.BPMNActivityEvent;
import org.activiti.runtime.api.event.CloudBPMNActivityEvent;
import org.activiti.runtime.api.event.CloudBPMNActivityStarted;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskAssignedEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityStartedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.activiti.runtime.api.event.impl.CloudRuntimeEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskAssignedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCreatedEventImpl;
import org.activiti.runtime.api.model.impl.BPMNActivityImpl;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
import org.activiti.runtime.api.model.impl.TaskImpl;
import org.conf.activiti.runtime.IgnoredRuntimeEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class AuditServiceIT {

    @Autowired
    private EventsRestTemplate eventsRestTemplate;

    @Autowired
    private EventsRepository repository;

    @Autowired
    private MyProducer2 producer;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void findAllShouldReturnAllAvailableEvents() throws Exception {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFindAll();

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSameSizeAs(coveredEvents);
            for (CloudRuntimeEvent coveredEvent : coveredEvents) {

                assertThat(retrievedEvents)
                        .extracting(
                                CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getServiceName,
                                CloudRuntimeEvent::getServiceVersion)
                        .contains(tuple(coveredEvent.getEventType(),
                                        coveredEvent.getServiceName(),
                                        coveredEvent.getServiceVersion()));
            }
        });
    }

    @Test
    public void shouldBeAbleToFilterOnProcessInstanceId() throws Exception {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("processInstanceId",
                                                                                                                                             "4"));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(2);
            for(CloudRuntimeEvent event : retrievedEvents) {
                CloudBPMNActivityEvent cloudBPMNActivityEvent = (CloudBPMNActivityEvent) event;
                assertThat(cloudBPMNActivityEvent.getProcessInstanceId()).isEqualTo("4");
            }
        });
    }


    @Test
    public void shouldBeAbleToFilterOnProcessInstanceIdAndEventType() throws Exception {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("processInstanceId", "4");
            filters.put("eventType", BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name());
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(2);
            for(CloudRuntimeEvent event : retrievedEvents) {
                CloudBPMNActivityEvent cloudBPMNActivityEvent = (CloudBPMNActivityStarted) event;
                assertThat(cloudBPMNActivityEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
                assertThat(cloudBPMNActivityEvent.getProcessInstanceId()).isEqualTo("4");
            }
        });
    }


    @Test
    public void shouldBeAbleToFilterOnEventType() throws Exception {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                             TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name()));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            CloudTaskAssignedEvent cloudTaskAssignedEvent = (CloudTaskAssignedEvent) retrievedEvents.iterator().next();
            assertThat(cloudTaskAssignedEvent.getEventType()).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED);
            assertThat(cloudTaskAssignedEvent.getEntity().getProcessDefinitionId()).isEqualTo("27");
            assertThat(cloudTaskAssignedEvent.getEntity().getProcessInstanceId()).isEqualTo("46");
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventTypeActivitiStarted() throws Exception {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                             BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name()));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(3);
            for(CloudRuntimeEvent event : retrievedEvents) {
                CloudBPMNActivityEvent cloudBPMNActivityEvent = (CloudBPMNActivityStarted) event;
                assertThat(cloudBPMNActivityEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            }
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventTypeTaskCancelled() throws Exception {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                             TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name()));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);

        });
    }

    @Test
    public void findByIdShouldReturnTheEventIdentifiedByTheGivenId() throws Exception {
        //given
        CloudRuntimeEvent[] events = new CloudRuntimeEvent[1];

        BPMNActivityImpl bpmnActivityStarted = new BPMNActivityImpl();
        bpmnActivityStarted.setActivityName("first step");
        String eventId = "ActivityStartedEventId" + UUID.randomUUID().toString();
        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent = new CloudBPMNActivityStartedEventImpl(eventId,
                                                                                                                System.currentTimeMillis(),
                                                                                                                bpmnActivityStarted);
        cloudBPMNActivityStartedEvent.setProcessDefinitionId("3");
        cloudBPMNActivityStartedEvent.setProcessInstanceId("4");

        events[0] = cloudBPMNActivityStartedEvent;

        producer.send(events);

        await().untilAsserted(() -> {

            //when
            ResponseEntity<CloudRuntimeEvent> responseEntity = eventsRestTemplate.executeFindById(eventId);

            //then
            CloudRuntimeEvent event = responseEntity.getBody();

            assertThat(event).isInstanceOf(CloudBPMNActivityStarted.class);

            CloudBPMNActivityStarted cloudProcessStartedEvent = (CloudBPMNActivityStarted) event;
            assertThat(cloudProcessStartedEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            assertThat(cloudProcessStartedEvent.getProcessDefinitionId()).isEqualTo("3");
            assertThat(cloudProcessStartedEvent.getProcessInstanceId()).isEqualTo("4");
            assertThat(cloudProcessStartedEvent.getEntity().getActivityName()).isEqualTo("first step");
        });
    }

    @Test
    public void unknownEventShouldNotPreventHandlingOfKnownEvents() throws Exception {
        //given
        CloudRuntimeEvent[] events = new CloudRuntimeEvent[2];

        BPMNActivityImpl bpmnActivityStarted = new BPMNActivityImpl();
        bpmnActivityStarted.setActivityName("first step");
        String eventId = "ActivityStartedEventId" + UUID.randomUUID().toString();
        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent = new CloudBPMNActivityStartedEventImpl(eventId,
                                                                                                                System.currentTimeMillis(),
                                                                                                                bpmnActivityStarted);
        cloudBPMNActivityStartedEvent.setProcessDefinitionId("3");
        cloudBPMNActivityStartedEvent.setProcessInstanceId("4");

        events[0] = cloudBPMNActivityStartedEvent;
        events[1] = new CloudRuntimeEventImpl() {
            @Override
            public Enum<?> getEventType() {
                return IgnoredRuntimeEvent.IgnoredRuntimeEvents.IGNORED;
            }
        };

        producer.send(events);

        await().untilAsserted(() -> {
            //then
            ResponseEntity<PagedResources<CloudRuntimeEvent>> eventsPagedResources = eventsRestTemplate.executeFindAll();
            assertThat(eventsPagedResources.getBody()).isNotEmpty();

            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            CloudRuntimeEvent event = retrievedEvents.iterator().next();
            //when
            assertThat(event).isInstanceOf(CloudBPMNActivityStarted.class);

            CloudBPMNActivityStarted cloudProcessStartedEvent = (CloudBPMNActivityStarted) event;
            assertThat(cloudProcessStartedEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            assertThat(cloudProcessStartedEvent.getProcessDefinitionId()).isEqualTo("3");
            assertThat(cloudProcessStartedEvent.getProcessInstanceId()).isEqualTo("4");
            assertThat(cloudProcessStartedEvent.getEntity().getActivityName()).isEqualTo("first step");
        });
    }

    private List<CloudRuntimeEvent> getTestEvents() {
        List<CloudRuntimeEvent> testEvents = new ArrayList<>();

        BPMNActivityImpl bpmnActivityCancelled = new BPMNActivityImpl();

        CloudBPMNActivityCancelledEventImpl cloudBPMNActivityCancelledEvent = new CloudBPMNActivityCancelledEventImpl("ActivityCancelledEventId",
                                                                                                                      System.currentTimeMillis(),
                                                                                                                      bpmnActivityCancelled);
        cloudBPMNActivityCancelledEvent.setProcessDefinitionId("103");
        cloudBPMNActivityCancelledEvent.setProcessInstanceId("104");

        testEvents.add(cloudBPMNActivityCancelledEvent);

        BPMNActivityImpl bpmnActivityStarted = new BPMNActivityImpl("1", "My Service Task", "Service Task");


        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent = new CloudBPMNActivityStartedEventImpl("ActivityStartedEventId",
                                                                                                                System.currentTimeMillis(),
                                                                                                                bpmnActivityStarted);
        cloudBPMNActivityStartedEvent.setProcessDefinitionId("3");
        cloudBPMNActivityStartedEvent.setProcessInstanceId("4");

        testEvents.add(cloudBPMNActivityStartedEvent);

        BPMNActivityImpl bpmnActivityStarted2 = new BPMNActivityImpl("2", "My User Task", "User Task");


        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent2 = new CloudBPMNActivityStartedEventImpl("ActivityStartedEventId2",
                                                                                                                System.currentTimeMillis(),
                                                                                                                 bpmnActivityStarted2);
        cloudBPMNActivityStartedEvent2.setProcessDefinitionId("3");
        cloudBPMNActivityStartedEvent2.setProcessInstanceId("4");

        testEvents.add(cloudBPMNActivityStartedEvent2);

        BPMNActivityImpl bpmnActivityStarted3 = new BPMNActivityImpl("2", "My User Task", "User Task");


        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent3 = new CloudBPMNActivityStartedEventImpl("ActivityStartedEventId3",
                                                                                                                 System.currentTimeMillis(),
                                                                                                                 bpmnActivityStarted3);
        cloudBPMNActivityStartedEvent3.setProcessDefinitionId("3");
        cloudBPMNActivityStartedEvent3.setProcessInstanceId("5");

        testEvents.add(cloudBPMNActivityStartedEvent3);

        BPMNActivityImpl bpmnActivityCompleted = new BPMNActivityImpl();

        CloudBPMNActivityCompletedEventImpl cloudBPMNActivityCompletedEvent = new CloudBPMNActivityCompletedEventImpl("ActivityCompletedEventId",
                                                                                                                      System.currentTimeMillis(),
                                                                                                                      bpmnActivityCompleted);
        cloudBPMNActivityCompletedEvent.setProcessDefinitionId("23");
        cloudBPMNActivityCompletedEvent.setProcessInstanceId("42");

        testEvents.add(cloudBPMNActivityCompletedEvent);

        ProcessInstanceImpl processInstanceCompleted = new ProcessInstanceImpl();
        processInstanceCompleted.setId("24");
        processInstanceCompleted.setProcessDefinitionId("43");

        CloudProcessCompletedEventImpl cloudProcessCompletedEvent = new CloudProcessCompletedEventImpl("ProcessCompletedEventId",
                                                                                                       System.currentTimeMillis(),
                                                                                                       processInstanceCompleted);

        testEvents.add(cloudProcessCompletedEvent);

        ProcessInstanceImpl processInstanceCancelled = new ProcessInstanceImpl();
        processInstanceCancelled.setId("124");
        processInstanceCancelled.setProcessDefinitionId("143");

        CloudProcessCancelledEventImpl cloudProcessCancelledEvent = new CloudProcessCancelledEventImpl("ProcessCancelledEventId",
                                                                                                       System.currentTimeMillis(),
                                                                                                       processInstanceCancelled);

        testEvents.add(cloudProcessCancelledEvent);

        ProcessInstanceImpl processInstanceStarted = new ProcessInstanceImpl();
        processInstanceStarted.setId("25");
        processInstanceStarted.setProcessDefinitionId("44");

        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl("ProcessStartedEventId",
                                                                                                 System.currentTimeMillis(),
                                                                                                 processInstanceStarted);

        testEvents.add(cloudProcessStartedEvent);

        TaskImpl taskAssigned = new TaskImpl();
        taskAssigned.setProcessDefinitionId("27");
        taskAssigned.setProcessInstanceId("46");
        CloudTaskAssignedEventImpl cloudTaskAssignedEvent = new CloudTaskAssignedEventImpl("TaskAssignedEventId",
                                                                                           System.currentTimeMillis(),
                                                                                           taskAssigned);
        testEvents.add(cloudTaskAssignedEvent);

        TaskImpl taskCompleted = new TaskImpl();
        taskCompleted.setProcessDefinitionId("28");
        taskCompleted.setProcessInstanceId("47");
        CloudTaskCompletedEventImpl cloudTaskCompletedEvent = new CloudTaskCompletedEventImpl("TaskCompletedEventId",
                                                                                              System.currentTimeMillis(),
                                                                                              taskCompleted);
        testEvents.add(cloudTaskCompletedEvent);

        TaskImpl taskCreated = new TaskImpl();
        taskCreated.setProcessDefinitionId("28");
        taskCreated.setProcessInstanceId("47");
        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl("TaskCreatedEventId",
                                                                                        System.currentTimeMillis(),
                                                                                        taskCreated);
        testEvents.add(cloudTaskCreatedEvent);


        TaskImpl taskCancelled = new TaskImpl();
        taskCancelled.setProcessDefinitionId("28");
        taskCancelled.setProcessInstanceId("47");
        CloudTaskCancelledEventImpl cloudTaskCancelledEvent = new CloudTaskCancelledEventImpl("TaskCancelledEventId",
                                                                                            System.currentTimeMillis(),
                                                                                            taskCancelled);
        testEvents.add(cloudTaskCancelledEvent);



//        VariableInstanceImpl variableCreated = new VariableInstanceImpl("name", "string", "value of string", "49" );
//        CloudVariableCreatedEventImpl cloudVariableCreatedEvent = new CloudVariableCreatedEventImpl("VariableCreatedEventId",
//                                                                                                    System.currentTimeMillis(),
//                                                                                                    variableCreated);
//        testEvents.add(cloudVariableCreatedEvent);
//
//        VariableInstanceImpl variableDeleted = new VariableInstanceImpl("name", "string", "value of string", "50" );
//        CloudVariableDeletedEventImpl cloudVariableDeletedEvent = new CloudVariableDeletedEventImpl("VariableDeletedEventId",
//                                                                                                    System.currentTimeMillis(),
//                                                                                                    variableDeleted);
//        testEvents.add(cloudVariableDeletedEvent);
//
//        VariableInstanceImpl variableUpdated = new VariableInstanceImpl("name", "string", "value of string", "51" );
//        CloudVariableUpdatedEventImpl cloudVariableUpdatedEvent = new CloudVariableUpdatedEventImpl("VariableUpdatedEventId",
//                                                                                                    System.currentTimeMillis(),
//                                                                                                    variableUpdated);
//        testEvents.add(cloudVariableUpdatedEvent);

//  String processDefinitionId, String processInstanceId

//        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(), "SequenceFlowTakenEvent", "14", "26", "45"));

//        coveredEvents.add(((MockIntegrationEventBuilder)((MockIntegrationEventBuilder)((MockIntegrationEventBuilder)MockIntegrationEventBuilder.anIntegrationRequestSentEvent().withExecutionId("21")).withProcessDefinitionId("33")).withProcessInstanceId("52")).withFlowNodeId("serviceTask").build());
//        coveredEvents.add(((MockIntegrationEventBuilder)((MockIntegrationEventBuilder)((MockIntegrationEventBuilder)MockIntegrationEventBuilder.anIntegrationResultRecievedEvent().withExecutionId("22")).withProcessDefinitionId("33")).withProcessInstanceId("52")).withFlowNodeId("serviceTask").build());

        return testEvents;
    }
}
