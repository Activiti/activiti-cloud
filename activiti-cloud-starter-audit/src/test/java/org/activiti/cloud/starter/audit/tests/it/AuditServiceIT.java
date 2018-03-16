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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.audit.events.ActivityStartedEventEntity;
import org.activiti.cloud.services.audit.events.ActivityStartedEventEntityAssert;
import org.activiti.cloud.services.audit.events.ProcessEngineEventEntity;
import org.activiti.cloud.services.audit.events.TaskAssignedEventEntity;
import org.activiti.cloud.services.audit.events.TaskAssignedEventEntityAssert;
import org.activiti.cloud.services.audit.repository.EventsRepository;
import org.activiti.cloud.starters.test.MockEventsSamples;
import org.activiti.cloud.starters.test.MockProcessEngineEvent;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starters.test.builder.ActivityEventBuilder.aActivityStartedEvent;
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
    private MyProducer producer;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void findAllShouldReturnAllAvailableEvents() throws Exception {
        //given
        List<ProcessEngineEvent> coveredEvents = MockEventsSamples.allSupportedEvents();
        producer.send(coveredEvents.toArray(new ProcessEngineEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessEngineEventEntity>> eventsPagedResources = eventsRestTemplate.executeFindAll();

            //then
            Collection<ProcessEngineEventEntity> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSameSizeAs(coveredEvents);
            for (ProcessEngineEvent coveredEvent : coveredEvents) {
                assertThat(retrievedEvents)
                        .extracting(
                                ProcessEngineEventEntity::getEventType,
                                ProcessEngineEventEntity::getExecutionId,
                                ProcessEngineEventEntity::getProcessDefinitionId,
                                ProcessEngineEventEntity::getProcessInstanceId)
                        .contains(tuple(coveredEvent.getEventType(),
                                        coveredEvent.getExecutionId(),
                                        coveredEvent.getProcessDefinitionId(),
                                        coveredEvent.getProcessInstanceId()));
            }
        });
    }

    @Test
    public void shouldBeAbleToFilterOnProcessInstanceId() throws Exception {
        //given
        List<ProcessEngineEvent> coveredEvents = MockEventsSamples.allSupportedEvents();
        producer.send(coveredEvents.toArray(new ProcessEngineEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessEngineEventEntity>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("processInstanceId",
                                                                                                                                                    "4"));

            //then
            Collection<ProcessEngineEventEntity> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            ActivityStartedEventEntityAssert.assertThat((ActivityStartedEventEntity) retrievedEvents.iterator().next())
                    .hasEventType("ActivityStartedEvent")
                    .hasExecutionId("2")
                    .hasProcessDefinitionId("3")
                    .hasProcessInstanceId("4");
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventType() throws Exception {
        //given
        List<ProcessEngineEvent> coveredEvents = MockEventsSamples.allSupportedEvents();
        producer.send(coveredEvents.toArray(new ProcessEngineEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessEngineEventEntity>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                                    "TaskAssignedEvent"));

            //then
            Collection<ProcessEngineEventEntity> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            TaskAssignedEventEntityAssert.assertThat((TaskAssignedEventEntity) retrievedEvents.iterator().next())
                    .hasEventType("TaskAssignedEvent")
                    .hasExecutionId("15")
                    .hasProcessDefinitionId("27")
                    .hasProcessInstanceId("46");
        });
    }

    @Test
    public void findByIdShouldReturnTheEventIdentifiedByTheGivenId() throws Exception {
        //given
        ProcessEngineEvent[] events = new ProcessEngineEvent[1];
        events[0] = aActivityStartedEvent(System.currentTimeMillis())
                .withExecutionId("2")
                .withProcessDefinitionId("3")
                .withProcessInstanceId("4")
                .withName("first step")
                .build();
        producer.send(events);

        await().untilAsserted(() -> {
            ResponseEntity<PagedResources<ProcessEngineEventEntity>> eventsPagedResources = eventsRestTemplate.executeFindAll();
            assertThat(eventsPagedResources.getBody().getContent()).isNotEmpty();
            ProcessEngineEventEntity event = eventsPagedResources.getBody().getContent().iterator().next();

            //when
            ResponseEntity<ProcessEngineEventEntity> responseEntity = eventsRestTemplate.executeFindById(event.getId());

            //then
            assertThat(responseEntity.getBody()).isInstanceOf(ActivityStartedEventEntity.class);
            ActivityStartedEventEntityAssert.assertThat((ActivityStartedEventEntity) responseEntity.getBody())
                    .hasId(event.getId())
                    .hasEventType("ActivityStartedEvent")
                    .hasExecutionId("2")
                    .hasProcessDefinitionId("3")
                    .hasProcessInstanceId("4")
                    .hasActivityName("first step");
        });
    }

    @Test
    public void unknownEventShouldNotPreventHandlingOfKnownEvents() throws Exception {
        //given
        ProcessEngineEvent[] events = new ProcessEngineEvent[2];
        events[0] = aActivityStartedEvent(System.currentTimeMillis())
                .withExecutionId("2")
                .withProcessDefinitionId("3")
                .withProcessInstanceId("4")
                .withName("first step")
                .build();
        events[1] = new MockProcessEngineEvent(System.currentTimeMillis(), "unknownType");
        producer.send(events);

        await().untilAsserted(() -> {
            //then
            ResponseEntity<PagedResources<ProcessEngineEventEntity>> eventsPagedResources = eventsRestTemplate.executeFindAll();
            assertThat(eventsPagedResources.getBody()).isNotEmpty();
            ProcessEngineEventEntity event = eventsPagedResources.getBody().iterator().next();

            //when
            assertThat(event).isInstanceOf(ActivityStartedEventEntity.class);
            ActivityStartedEventEntityAssert.assertThat((ActivityStartedEventEntity) event)
                    .hasEventType("ActivityStartedEvent")
                    .hasExecutionId("2")
                    .hasProcessDefinitionId("3")
                    .hasProcessInstanceId("4")
                    .hasActivityName("first step");
        });
    }
}
