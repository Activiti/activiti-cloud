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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.api.process.model.events.MessageSubscriptionCancelledEvent;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.TimerPayload;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.api.runtime.model.impl.BPMNErrorImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.MessageSubscriptionImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.conf.IgnoredRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudMessageSubscriptionCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCancelledEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource("classpath:application.properties")
@Import(EventsRestTemplate.class)
public class AuditServiceIT {

    @Autowired
    private EventsRestTemplate eventsRestTemplate;

    @Autowired
    private EventsRepository repository;

    @Autowired
    private MyProducer producer;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    public void findAllShouldReturnAllAvailableEvents() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFindAll();

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
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
            assertThatEntityIsSet(retrievedEvents);
        });
    }

    @Test
    public void shouldBeAbleToFilterOnProcessInstanceId() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(Collections.singletonMap("processInstanceId",
                                                                                                                                             "4"));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(2);
            for (CloudRuntimeEvent event : retrievedEvents) {
                CloudBPMNActivityEvent cloudBPMNActivityEvent = (CloudBPMNActivityEvent) event;
                assertThat(cloudBPMNActivityEvent.getProcessInstanceId()).isEqualTo("4");
            }
        });
    }

    @Test
    public void shouldBeAbleToFilterOnProcessInstanceIdAndEventType() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("processInstanceId",
                        "4");
            filters.put("eventType",
                        BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name());
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(2);
            for (CloudRuntimeEvent event : retrievedEvents) {
                CloudBPMNActivityEvent cloudBPMNActivityEvent = (CloudBPMNActivityStartedEvent) event;
                assertThat(cloudBPMNActivityEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            }
        });
    }

    @Test
    public void should_getTaskCreatedEvent_when_filteredOnProcessInstanceIdEventType() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("processInstanceId",
                        "47");
            filters.put("eventType",
                        TaskRuntimeEvent.TaskEvents.TASK_CREATED.name());

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            assertThat(eventsPagedModel).isNotNull();
            assertThat(eventsPagedModel.getBody())
                .isNotEmpty()
                .filteredOn(event -> ((CloudTaskCreatedEvent)event).getEntity().getTaskDefinitionKey().equals("taskDefinitionKey"))
                .extracting(event -> event.getEventType(),
                            event -> ((CloudTaskCreatedEvent)event).getEntity().getName(),
                            event -> ((CloudTaskCreatedEvent)event).getEntity().getTaskDefinitionKey())
                .contains(  tuple(TaskRuntimeEvent.TaskEvents.TASK_CREATED,
                                  "task created",
                                  "taskDefinitionKey"));

        });
    }

    @Test
    public void shouldGetProcessStartedUpdatedCompletedEvents() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestProcessStartedUpdatedCompletedEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[0]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("processInstanceId",
                        "25");


            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
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

            assertThatEntityIsSet(retrievedEvents);
            retrievedEvents.forEach(event -> {
                assertThat(event.getProcessDefinitionId())
                        .describedAs("Process definition for event " + event.getEventType() + " should not be null!")
                        .isNotNull();
                assertThat(event.getProcessInstanceId())
                        .describedAs("Process instance for event " + event.getEventType() + " should not be null!")
                        .isNotNull();
            });
        });
    }

    private void assertThatEntityIsSet(Collection<CloudRuntimeEvent> retrievedEvents) {
        retrievedEvents.forEach(
                event -> assertThat(event.getEntity())
                        .describedAs("Entity for " + event.getEventType() + " should not be null")
                        .isNotNull()
        );
        retrievedEvents.forEach(
                event -> assertThat(event.getEntityId())
                        .describedAs("EntityId for " + event.getEventType() + " should not be null")
                        .isNotNull()
        );
    }

    @Test
    public void shouldGetEventsForACancelledTask() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTaskCancelledEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("entityId",
                        "1234-abc-5678-def");

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(3);
            for (CloudRuntimeEvent e : retrievedEvents) {
                assertThat(e.getEntityId()).isEqualTo("1234-abc-5678-def");
            }
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventType() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                             TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name()));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            CloudTaskAssignedEvent cloudTaskAssignedEvent = (CloudTaskAssignedEvent) retrievedEvents.iterator().next();
            assertThat(cloudTaskAssignedEvent.getEventType()).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED);
            assertThat(cloudTaskAssignedEvent.getEntity().getProcessDefinitionId()).isEqualTo("27");
            assertThat(cloudTaskAssignedEvent.getEntity().getProcessInstanceId()).isEqualTo("46");
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventTypeActivitiStarted() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                             BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name()));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(3);
            for (CloudRuntimeEvent event : retrievedEvents) {
                CloudBPMNActivityEvent cloudBPMNActivityEvent = (CloudBPMNActivityStartedEvent) event;
                assertThat(cloudBPMNActivityEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            }
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventEntityId() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(Collections.singletonMap("entityId",
                                                                                                                                             "1234-abc-5678-def"));
            //then
            assertThat(eventsPagedModel.getBody()).isNotNull();

            assertThat(eventsPagedModel.getBody().getContent())
                    .extracting(CloudRuntimeEvent::getEntityId)
                    .containsOnly("1234-abc-5678-def");
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventTypeTaskCancelled() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                             TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name()));

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            CloudTaskCancelledEvent cloudBPMNTaskCancelled = (CloudTaskCancelledEvent) retrievedEvents.iterator().next();
            assertThat(cloudBPMNTaskCancelled.getEventType()).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_CANCELLED);
            assertThat(cloudBPMNTaskCancelled.getEntityId()).isEqualTo("1234-abc-5678-def");
            assertThat(cloudBPMNTaskCancelled.getEntity()).isInstanceOf(Task.class);
            assertThat(cloudBPMNTaskCancelled.getEntity().getId()).isEqualTo("1234-abc-5678-def");
        });
    }

    @Test
    public void findByIdShouldReturnTheEventIdentifiedByTheGivenId() {
        //given
        CloudRuntimeEvent[] events = new CloudRuntimeEvent[1];

        BPMNActivityImpl bpmnActivityStarted = new BPMNActivityImpl();
        bpmnActivityStarted.setActivityName("first step");
        String eventId = "ActivityStartedEventId" + UUID.randomUUID().toString();
        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent = new CloudBPMNActivityStartedEventImpl(eventId,
                                                                                                                System.currentTimeMillis(),
                                                                                                                bpmnActivityStarted,
                                                                                                                "3",
                                                                                                                "4");

        events[0] = cloudBPMNActivityStartedEvent;

        producer.send(events);

        await().untilAsserted(() -> {

            //when
            ResponseEntity<CloudRuntimeEvent> responseEntity = eventsRestTemplate.executeFindById(eventId);

            //then
            CloudRuntimeEvent event = responseEntity.getBody();

            assertThat(event).isInstanceOf(CloudBPMNActivityStartedEvent.class);

            CloudBPMNActivityStartedEvent cloudProcessStartedEvent = (CloudBPMNActivityStartedEvent) event;
            assertThat(cloudProcessStartedEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            assertThat(cloudProcessStartedEvent.getProcessDefinitionId()).isEqualTo("3");
            assertThat(cloudProcessStartedEvent.getProcessInstanceId()).isEqualTo("4");
            assertThat(cloudProcessStartedEvent.getEntity().getActivityName()).isEqualTo("first step");
        });
    }

    @Test
    public void unknownEventShouldNotPreventHandlingOfKnownEvents() {
        //given
        CloudRuntimeEvent[] events = new CloudRuntimeEvent[2];

        BPMNActivityImpl bpmnActivityStarted = new BPMNActivityImpl();
        bpmnActivityStarted.setActivityName("first step");
        String eventId = "ActivityStartedEventId" + UUID.randomUUID().toString();
        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent = new CloudBPMNActivityStartedEventImpl(eventId,
                                                                                                                System.currentTimeMillis(),
                                                                                                                bpmnActivityStarted,
                                                                                                                "3",
                                                                                                                "4");

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
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFindAll();
            assertThat(eventsPagedModel.getBody()).isNotEmpty();

            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            CloudRuntimeEvent event = retrievedEvents.iterator().next();
            //when
            assertThat(event).isInstanceOf(CloudBPMNActivityStartedEvent.class);

            CloudBPMNActivityStartedEvent cloudProcessStartedEvent = (CloudBPMNActivityStartedEvent) event;
            assertThat(cloudProcessStartedEvent.getEventType()).isEqualTo(BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
            assertThat(cloudProcessStartedEvent.getProcessDefinitionId()).isEqualTo("3");
            assertThat(cloudProcessStartedEvent.getProcessInstanceId()).isEqualTo("4");
            assertThat(cloudProcessStartedEvent.getEntity().getActivityName()).isEqualTo("first step");
        });
    }

    @Test
    public void shouldGetUserCandidateEvents() {
        //given
        List<CloudRuntimeEvent> coveredEvents = getTestUserCandidatesEvents();
        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFindAll();

             //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(2);

            CloudRuntimeEvent e= retrievedEvents.iterator().next();
            assertThat(e.getEventType()).isIn(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED,TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED);
            assertThat(e.getEntityId()).isEqualTo("userId");

            e= retrievedEvents.iterator().next();
            assertThat(e.getEventType()).isIn(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED,TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED);
            assertThat(e.getEntityId()).isEqualTo("userId");

        });
    }

    @Test
    public void eventsShouldHaveSequenceNumberSet() {
        //given
        List <CloudRuntimeEvent> testEvents= getTaskCancelledEvents();

        producer.send(testEvents.toArray(new CloudRuntimeEvent[testEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFindAll();

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            List <CloudRuntimeEvent> retrievedEventsList = new ArrayList<>(retrievedEvents);

            assertThat(retrievedEvents).hasSameSizeAs(testEvents);

            for(int i = 0; i < testEvents.size(); i++ ){
                assertThat(retrievedEventsList.get(i).getSequenceNumber())
                        .isEqualTo(i);
            }

        });
    }

    @Test
    public void eventsShouldHaveMessageIdSet() {
        //given
        List <CloudRuntimeEvent> testEvents= getTaskCancelledEvents();

        producer.send(testEvents.toArray(new CloudRuntimeEvent[testEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFindAll();

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            List <CloudRuntimeEvent> retrievedEventsList = new ArrayList<>(retrievedEvents);

            assertThat(retrievedEvents).hasSameSizeAs(testEvents);
            String commonMessageId = retrievedEventsList.get(0).getMessageId();

            for(int i = 0; i < testEvents.size(); i++ ){
                assertThat(retrievedEventsList.get(i).getMessageId())
                        .isNotNull()
                        .isEqualTo(commonMessageId);
            }

        });
    }

    @Test
    public void shouldGetSignalReceivedEvent() {
        //given
        List<CloudRuntimeEvent> coveredEvents = new ArrayList<>();

        BPMNSignalImpl signal = new BPMNSignalImpl("signalId");
        signal.setProcessDefinitionId("processDefinitionId");
        signal.setProcessInstanceId("processInstanceId");

        SignalPayload signalPayload = ProcessPayloadBuilder.signal()
                .withName("SignalName")
                .withVariable("signal-variable",
                              "test")
                .build();
        signal.setSignalPayload(signalPayload);


        CloudBPMNSignalReceivedEventImpl cloudSignalReceivedEvent = new CloudBPMNSignalReceivedEventImpl("eventId",
                                                                                                         System.currentTimeMillis(),
                                                                                                         signal,
                                                                                                         signal.getProcessDefinitionId(),
                                                                                                         signal.getProcessInstanceId());
        coveredEvents.add(cloudSignalReceivedEvent);


        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("entityId",
                        "signalId");

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);

            assertThat(retrievedEvents)
            .extracting(
                    CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getServiceName,
                    CloudRuntimeEvent::getServiceVersion,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getEntityId,
                    event -> ((CloudBPMNSignalReceivedEvent)event).getEntity().getElementId(),
                    event -> ((CloudBPMNSignalReceivedEvent)event).getEntity().getSignalPayload().getId(),
                    event -> ((CloudBPMNSignalReceivedEvent)event).getEntity().getSignalPayload().getName(),
                    event -> ((CloudBPMNSignalReceivedEvent)event).getEntity().getSignalPayload().getVariables())
            .contains(tuple(cloudSignalReceivedEvent.getEventType(),
            		        cloudSignalReceivedEvent.getServiceName(),
            		        cloudSignalReceivedEvent.getServiceVersion(),
            		        cloudSignalReceivedEvent.getProcessInstanceId(),
            		        cloudSignalReceivedEvent.getProcessDefinitionId(),
            		        cloudSignalReceivedEvent.getEntityId(),
            		        cloudSignalReceivedEvent.getEntity().getElementId(),
            		        cloudSignalReceivedEvent.getEntity().getSignalPayload().getId(),
            		        cloudSignalReceivedEvent.getEntity().getSignalPayload().getName(),
            		        cloudSignalReceivedEvent.getEntity().getSignalPayload().getVariables()));
        });
    }

    @Test
    public void shouldGetTimerScheduledEvent() {
        //given
        List<CloudRuntimeEvent> coveredEvents = new ArrayList<>();

        BPMNTimerImpl timer1 = new BPMNTimerImpl("timerId1");
        timer1.setProcessDefinitionId("processDefinitionId");
        timer1.setProcessInstanceId("processInstanceId");
        timer1.setTimerPayload(createTimerPayload());

        CloudBPMNTimerFiredEventImpl cloudTimerFiredEvent = new CloudBPMNTimerFiredEventImpl("eventId1",
                                                                                             System.currentTimeMillis(),
                                                                                             timer1,
                                                                                             timer1.getProcessDefinitionId(),
                                                                                             timer1.getProcessInstanceId());
        coveredEvents.add(cloudTimerFiredEvent);

        BPMNTimerImpl timer2 = new BPMNTimerImpl("timerId2");
        timer2.setProcessDefinitionId("processDefinitionId");
        timer2.setProcessInstanceId("processInstanceId");
        timer2.setTimerPayload(createTimerPayload());

        CloudBPMNTimerScheduledEventImpl cloudTimerScheduledEvent = new CloudBPMNTimerScheduledEventImpl("eventId2",
                                                                                                         System.currentTimeMillis(),
                                                                                                         timer2,
                                                                                                         timer2.getProcessDefinitionId(),
                                                                                                         timer2.getProcessInstanceId());

        coveredEvents.add(cloudTimerScheduledEvent);

        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("eventType",
                        BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED.name());

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);

            assertThat(retrievedEvents)
            .extracting(
                    CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getServiceName,
                    CloudRuntimeEvent::getServiceVersion,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getEntityId,
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getElementId(),
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getProcessInstanceId(),
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getProcessDefinitionId(),
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getTimerPayload().getId(),
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getTimerPayload().getMaxIterations(),
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getTimerPayload().getRepeat(),
                    event -> ((CloudBPMNTimerScheduledEvent)event).getEntity().getTimerPayload().getRetries())
            .contains(tuple(cloudTimerScheduledEvent.getEventType(),
                            cloudTimerScheduledEvent.getServiceName(),
                            cloudTimerScheduledEvent.getServiceVersion(),
                            cloudTimerScheduledEvent.getProcessInstanceId(),
                            cloudTimerScheduledEvent.getProcessDefinitionId(),
                            cloudTimerScheduledEvent.getEntityId(),
                            cloudTimerScheduledEvent.getEntity().getElementId(),
                            cloudTimerScheduledEvent.getEntity().getProcessInstanceId(),
                            cloudTimerScheduledEvent.getEntity().getProcessDefinitionId(),
                            cloudTimerScheduledEvent.getEntity().getTimerPayload().getId(),
                            cloudTimerScheduledEvent.getEntity().getTimerPayload().getMaxIterations(),
                            cloudTimerScheduledEvent.getEntity().getTimerPayload().getRepeat(),
                            cloudTimerScheduledEvent.getEntity().getTimerPayload().getRetries()));
        });
    }

    @Test
    public void shouldGetErrorReceivedEvent() {
        //given
        List<CloudRuntimeEvent> coveredEvents = new ArrayList<>();

        BPMNErrorImpl error = new BPMNErrorImpl("elementId");
        error.setProcessDefinitionId("processDefinitionId");
        error.setProcessInstanceId("processInstanceId");
        error.setErrorCode("errorCode");
        error.setErrorId("errorId");

        CloudBPMNErrorReceivedEventImpl cloudErrorReceivedEvent = new CloudBPMNErrorReceivedEventImpl("eventId",
                                                                                                       System.currentTimeMillis(),
                                                                                                       error,
                                                                                                       error.getProcessDefinitionId(),
                                                                                                       error.getProcessInstanceId());
        coveredEvents.add(cloudErrorReceivedEvent);

        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("eventType",
                        BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED.name());

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);

            assertThat(retrievedEvents)
            .extracting(
                    CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getServiceName,
                    CloudRuntimeEvent::getServiceVersion,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getEntityId,
                    event -> ((CloudBPMNErrorReceivedEvent)event).getEntity().getElementId(),
                    event -> ((CloudBPMNErrorReceivedEvent)event).getEntity().getProcessInstanceId(),
                    event -> ((CloudBPMNErrorReceivedEvent)event).getEntity().getProcessDefinitionId(),
                    event -> ((CloudBPMNErrorReceivedEvent)event).getEntity().getErrorCode(),
                    event -> ((CloudBPMNErrorReceivedEvent)event).getEntity().getErrorId())
            .contains(tuple(cloudErrorReceivedEvent.getEventType(),
                            cloudErrorReceivedEvent.getServiceName(),
                            cloudErrorReceivedEvent.getServiceVersion(),
                            cloudErrorReceivedEvent.getProcessInstanceId(),
                            cloudErrorReceivedEvent.getProcessDefinitionId(),
                            cloudErrorReceivedEvent.getEntityId(),
                            cloudErrorReceivedEvent.getEntity().getElementId(),
                            cloudErrorReceivedEvent.getEntity().getProcessInstanceId(),
                            cloudErrorReceivedEvent.getEntity().getProcessDefinitionId(),
                            cloudErrorReceivedEvent.getEntity().getErrorCode(),
                            cloudErrorReceivedEvent.getEntity().getErrorId()));
        });
    }

    @Test
    public void shouldGetMessageSubscriptionCancelledEvent() {
        //given
        List<CloudRuntimeEvent> coveredEvents = new ArrayList<>();

        MessageSubscriptionImpl messageSubscription = MessageSubscriptionImpl.builder()
                .withId("entityId")
                .withEventName("messageName")
                .withConfiguration("correlationKey")
                .withProcessDefinitionId("processDefinitionId")
                .withProcessInstanceId("processInstanceId")
                .withBusinessKey("businessKey")
                .build();

        CloudMessageSubscriptionCancelledEventImpl cloudEvent = CloudMessageSubscriptionCancelledEventImpl.builder()
                                                                                .withEntity(messageSubscription)
                                                                                .build();
        coveredEvents.add(cloudEvent);

        producer.send(coveredEvents.toArray(new CloudRuntimeEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            Map<String, Object> filters = new HashMap<>();
            filters.put("eventType",
                        MessageSubscriptionCancelledEvent.MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED.name());

            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate.executeFind(filters);

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);

            assertThat(retrievedEvents)
            .extracting(
                    CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getServiceName,
                    CloudRuntimeEvent::getServiceVersion,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getEntityId,
                    event -> ((CloudMessageSubscriptionCancelledEvent)event).getEntity().getId(),
                    event -> ((CloudMessageSubscriptionCancelledEvent)event).getEntity().getProcessInstanceId(),
                    event -> ((CloudMessageSubscriptionCancelledEvent)event).getEntity().getProcessDefinitionId(),
                    event -> ((CloudMessageSubscriptionCancelledEvent)event).getEntity().getEventName(),
                    event -> ((CloudMessageSubscriptionCancelledEvent)event).getEntity().getConfiguration(),
                    event -> ((CloudMessageSubscriptionCancelledEvent)event).getEntity().getBusinessKey())
            .contains(tuple(cloudEvent.getEventType(),
                            cloudEvent.getServiceName(),
                            cloudEvent.getServiceVersion(),
                            cloudEvent.getProcessInstanceId(),
                            cloudEvent.getProcessDefinitionId(),
                            cloudEvent.getEntityId(),
                            cloudEvent.getEntity().getId(),
                            cloudEvent.getEntity().getProcessInstanceId(),
                            cloudEvent.getEntity().getProcessDefinitionId(),
                            cloudEvent.getEntity().getEventName(),
                            cloudEvent.getEntity().getConfiguration(),
                            cloudEvent.getEntity().getBusinessKey()));
        });
    }

    private List<CloudRuntimeEvent> getTaskCancelledEvents() {
        List<CloudRuntimeEvent> testEvents = new ArrayList<>();
        TaskImpl taskCreated = new TaskImpl("1234-abc-5678-def",
                                            "my task",
                                            Task.TaskStatus.CREATED);
        taskCreated.setProcessDefinitionId("proc-def");
        taskCreated.setProcessInstanceId("100");
        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl("TaskCreatedEventId",
                                                                                        System.currentTimeMillis(),
                                                                                        taskCreated);
        testEvents.add(cloudTaskCreatedEvent);

        TaskImpl taskAssigned = new TaskImpl("1234-abc-5678-def",
                                             "my task",
                                             Task.TaskStatus.ASSIGNED);
        taskAssigned.setProcessDefinitionId("proc-def");
        taskAssigned.setProcessInstanceId("100");
        CloudTaskAssignedEventImpl cloudTaskAssignedEvent = new CloudTaskAssignedEventImpl("TaskAssignedEventId",
                                                                                           System.currentTimeMillis(),
                                                                                           taskAssigned);
        testEvents.add(cloudTaskAssignedEvent);

        TaskImpl taskCancelled = new TaskImpl("1234-abc-5678-def",
                                              "my task",
                                              Task.TaskStatus.CANCELLED);
        taskCancelled.setProcessDefinitionId("proc-def");
        taskCancelled.setProcessInstanceId("100");
        CloudTaskCancelledEventImpl cloudTaskCancelledEvent = new CloudTaskCancelledEventImpl("TaskCancelledEventId",
                                                                                              System.currentTimeMillis(),
                                                                                              taskCancelled);
        testEvents.add(cloudTaskCancelledEvent);
        return testEvents;
    }

    private List<CloudRuntimeEvent> getTestEvents() {
        List<CloudRuntimeEvent> testEvents = new ArrayList<>();

        testEvents.add(new CloudProcessDeployedEventImpl(buildDefaultProcessDefinition()));

        BPMNActivityImpl bpmnActivityCancelled = new BPMNActivityImpl();
        bpmnActivityCancelled.setElementId("elementId");

        testEvents.add(new CloudBPMNActivityCancelledEventImpl("ActivityCancelledEventId",
                                                               System.currentTimeMillis(),
                                                               bpmnActivityCancelled,
                                                               "103",
                                                               "104",
                                                               "manually cancelled"));

        BPMNActivityImpl bpmnActivityStarted = new BPMNActivityImpl("1",
                                                                    "My Service Task",
                                                                    "Service Task");

        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent = new CloudBPMNActivityStartedEventImpl("ActivityStartedEventId",
                                                                                                                System.currentTimeMillis(),
                                                                                                                bpmnActivityStarted,
                                                                                                                "3",
                                                                                                                "4");

        testEvents.add(cloudBPMNActivityStartedEvent);

        BPMNActivityImpl bpmnActivityStarted2 = new BPMNActivityImpl("2",
                                                                     "My User Task",
                                                                     "User Task");

        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent2 = new CloudBPMNActivityStartedEventImpl("ActivityStartedEventId2",
                                                                                                                 System.currentTimeMillis(),
                                                                                                                 bpmnActivityStarted2,
                                                                                                                 "3",
                                                                                                                 "4");

        testEvents.add(cloudBPMNActivityStartedEvent2);

        BPMNActivityImpl bpmnActivityStarted3 = new BPMNActivityImpl("2",
                                                                     "My User Task",
                                                                     "User Task");

        CloudBPMNActivityStartedEventImpl cloudBPMNActivityStartedEvent3 = new CloudBPMNActivityStartedEventImpl("ActivityStartedEventId3",
                                                                                                                 System.currentTimeMillis(),
                                                                                                                 bpmnActivityStarted3,
                                                                                                                 "3",
                                                                                                                 "5");

        testEvents.add(cloudBPMNActivityStartedEvent3);

        BPMNActivityImpl bpmnActivityCompleted = new BPMNActivityImpl();
        bpmnActivityCompleted.setElementId("elementId");

        testEvents.add(new CloudBPMNActivityCompletedEventImpl("ActivityCompletedEventId",
                                                               System.currentTimeMillis(),
                                                               bpmnActivityCompleted,
                                                               "23",
                                                               "42"));

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

        testEvents.add(new CloudProcessSuspendedEventImpl("ProcessStartedEventId",
                                           System.currentTimeMillis(),
                                           processInstanceStarted));

        TaskImpl taskAssigned = new TaskImpl("1234-abc-5678-def",
                                             "task assigned",
                                             Task.TaskStatus.ASSIGNED);
        taskAssigned.setProcessDefinitionId("27");
        taskAssigned.setProcessInstanceId("46");
        CloudTaskAssignedEventImpl cloudTaskAssignedEvent = new CloudTaskAssignedEventImpl("TaskAssignedEventId",
                                                                                           System.currentTimeMillis(),
                                                                                           taskAssigned);
        testEvents.add(cloudTaskAssignedEvent);

        taskAssigned.setPriority(10);
        testEvents.add(new CloudTaskUpdatedEventImpl("TaskUpdatedEventId",
                                                     System.currentTimeMillis(),
                                                     taskAssigned));

        TaskImpl taskCompleted = new TaskImpl("1234-abc-5678-def",
                                              "task completed",
                                              Task.TaskStatus.COMPLETED);
        taskCompleted.setProcessDefinitionId("28");
        taskCompleted.setProcessInstanceId("47");
        CloudTaskCompletedEventImpl cloudTaskCompletedEvent = new CloudTaskCompletedEventImpl("TaskCompletedEventId",
                                                                                              System.currentTimeMillis(),
                                                                                              taskCompleted);
        testEvents.add(cloudTaskCompletedEvent);

        TaskImpl taskCreated = new TaskImpl("1234-abc-5678-def",
                                            "task created",
                                            Task.TaskStatus.CREATED);
        taskCreated.setProcessDefinitionId("28");
        taskCreated.setProcessInstanceId("47");
        taskCreated.setTaskDefinitionKey("taskDefinitionKey");
        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl("TaskCreatedEventId",
                                                                                        System.currentTimeMillis(),
                                                                                        taskCreated);
        testEvents.add(cloudTaskCreatedEvent);

        TaskImpl taskCancelled = new TaskImpl("1234-abc-5678-def",
                                              "task cancelled",
                                              Task.TaskStatus.CANCELLED);
        taskCancelled.setProcessDefinitionId("28");
        taskCancelled.setProcessInstanceId("47");
        CloudTaskCancelledEventImpl cloudTaskCancelledEvent = new CloudTaskCancelledEventImpl("TaskCancelledEventId",
                                                                                              System.currentTimeMillis(),
                                                                                              taskCancelled);
        testEvents.add(cloudTaskCancelledEvent);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setProcessDefinitionId("28");
        integrationContext.setProcessInstanceId("47");
        integrationContext.setClientId("elementId");
        integrationContext.setClientName("Connector Task");
        integrationContext.setClientType("serviceTask");

        CloudIntegrationRequestedEventImpl cloudIntegrationRequestedEvent = new CloudIntegrationRequestedEventImpl(integrationContext);

        testEvents.add(cloudIntegrationRequestedEvent);

        CloudIntegrationResultReceivedEventImpl cloudIntegrationResultReceivedEvent = new CloudIntegrationResultReceivedEventImpl(integrationContext);

        testEvents.add(cloudIntegrationResultReceivedEvent);

        Error error = new Error("Error Message");
        error.fillInStackTrace();

        CloudIntegrationErrorReceivedEventImpl cloudIntegrationErrorReceivedEvent = new CloudIntegrationErrorReceivedEventImpl(integrationContext,
                                                                                                                               error.getMessage(),
                                                                                                                               error.getClass().getName(),
                                                                                                                               Arrays.asList(error.getStackTrace()));
        testEvents.add(cloudIntegrationErrorReceivedEvent);

        return testEvents;
    }

    private ProcessDefinitionImpl buildDefaultProcessDefinition() {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setName("My process");
        processDefinition.setKey("my proc");
        processDefinition.setId("processId");
        return processDefinition;
    }

    private List<CloudRuntimeEvent> getTestProcessStartedUpdatedCompletedEvents() {
        List<CloudRuntimeEvent> testEvents = new ArrayList<>();

        ProcessInstanceImpl processInstanceStarted = new ProcessInstanceImpl();
        processInstanceStarted.setId("25");
        processInstanceStarted.setProcessDefinitionId("44");

        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl("ProcessStartedEventId",
                                                                                                 System.currentTimeMillis(),
                                                                                                 processInstanceStarted);

        testEvents.add(cloudProcessStartedEvent);

        ProcessInstanceImpl processInstanceUpdated = new ProcessInstanceImpl();
        processInstanceUpdated.setId("25");
        processInstanceUpdated.setProcessDefinitionId("44");
        processInstanceUpdated.setBusinessKey("businessKey");
        processInstanceUpdated.setName("name");

        CloudProcessUpdatedEventImpl cloudProcessUpdatedEvent = new CloudProcessUpdatedEventImpl("ProcessUpdatedEventId",
                                                                                                 System.currentTimeMillis(),
                                                                                                 processInstanceUpdated);
        testEvents.add(cloudProcessUpdatedEvent);

        ProcessInstanceImpl processInstanceCompleted = new ProcessInstanceImpl();
        processInstanceCompleted.setId("25");
        processInstanceCompleted.setProcessDefinitionId("44");

        CloudProcessCompletedEventImpl cloudProcessCompletedEvent = new CloudProcessCompletedEventImpl("ProcessCompletedEventId",
                                                                                                       System.currentTimeMillis(),
                                                                                                       processInstanceCompleted);

        testEvents.add(cloudProcessCompletedEvent);

        return testEvents;
    }

    private List<CloudRuntimeEvent> getTestUserCandidatesEvents() {
        List<CloudRuntimeEvent> testEvents = new ArrayList<>();

        TaskCandidateUserImpl taskCandidateUser=new TaskCandidateUserImpl("userId", "1234-abc-5678-def");

        CloudTaskCandidateUserAddedEventImpl candidateUserAddedEvent = new CloudTaskCandidateUserAddedEventImpl("TaskCandidateUserAddedEventId",
                                                                                                            System.currentTimeMillis(),
                                                                                                            taskCandidateUser);

        testEvents.add(candidateUserAddedEvent);

        taskCandidateUser=new TaskCandidateUserImpl("userId", "1234-abc-5678-def");
        CloudTaskCandidateUserRemovedEventImpl candidateUserRemovedEvent = new CloudTaskCandidateUserRemovedEventImpl("TaskCandidateUserRemovedEventId",
                                                                                                                  System.currentTimeMillis(),
                                                                                                                  taskCandidateUser);
        testEvents.add(candidateUserRemovedEvent);

        return testEvents;
    }

    private TimerPayload createTimerPayload() {
        TimerPayload timerPayload = new TimerPayload();
        timerPayload.setRetries(5);
        timerPayload.setMaxIterations(2);
        timerPayload.setRepeat("repeat");
        timerPayload.setExceptionMessage("Any message");

        return timerPayload;
    }
}
