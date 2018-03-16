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

package org.activiti.cloud.starter.audit.mongo.tests.it;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.audit.mongo.events.ActivityStartedEventDocument;
import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.activiti.cloud.services.audit.mongo.repository.EventsRepository;
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starters.test.builder.ActivityEventBuilder.aActivityStartedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
            ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsPagedResources = eventsRestTemplate.executeFindAll();

            //then
            Collection<ProcessEngineEventDocument> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSameSizeAs(coveredEvents);
            for (ProcessEngineEvent coveredEvent : coveredEvents) {
                assertThat(retrievedEvents)
                        .extracting(
                                ProcessEngineEventDocument::getEventType,
                                ProcessEngineEventDocument::getExecutionId,
                                ProcessEngineEventDocument::getProcessDefinitionId,
                                ProcessEngineEventDocument::getProcessInstanceId)
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
            ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("processInstanceId",
                                                                                                                                                      "4"));

            //then
            Collection<ProcessEngineEventDocument> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);
            ProcessEngineEventDocument event = retrievedEvents.iterator().next();
            assertThat(event.getEventType()).isEqualTo("ActivityStartedEvent");
            assertThat(event.getExecutionId()).isEqualTo("2");
            assertThat(event.getProcessDefinitionId()).isEqualTo("3");
            assertThat(event.getProcessInstanceId()).isEqualTo("4");
        });
    }

    @Test
    public void shouldBeAbleToFilterOnEventType() throws Exception {
        //given
        List<ProcessEngineEvent> coveredEvents = MockEventsSamples.allSupportedEvents();
        producer.send(coveredEvents.toArray(new ProcessEngineEvent[coveredEvents.size()]));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsPagedResources = eventsRestTemplate.executeFind(Collections.singletonMap("eventType",
                                                                                                                                                      "TaskAssignedEvent"));

            //then
            Collection<ProcessEngineEventDocument> retrievedEvents = eventsPagedResources.getBody().getContent();
            assertThat(retrievedEvents).hasSize(1);

            ProcessEngineEventDocument event = retrievedEvents.iterator().next();
            assertThat(event.getEventType()).isEqualTo("TaskAssignedEvent");
            assertThat(event.getExecutionId()).isEqualTo("15");
            assertThat(event.getProcessDefinitionId()).isEqualTo("27");
            assertThat(event.getProcessInstanceId()).isEqualTo("46");
        });
    }

    @Test
    public void findByIdShouldReturnTheEventIdentifiedByTheGivenId() throws Exception {
        //given
        ProcessEngineEvent[] events = new ProcessEngineEvent[1];
        events[0] = new MockProcessEngineEvent(System.currentTimeMillis(),
                                               "ActivityStartedEvent",
                                               "2",
                                               "3",
                                               "4");
        producer.send(events);

        await().untilAsserted(() -> {

            ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsPagedResources = eventsRestTemplate.executeFindAll();
            assertThat(eventsPagedResources.getBody().getContent()).isNotEmpty();
            ProcessEngineEventDocument event = eventsPagedResources.getBody()
                    .getContent()
                    .iterator()
                    .next();

            //when
            ResponseEntity<ProcessEngineEventDocument> responseEntity = eventsRestTemplate.executeFindById(event.getId());

            //then
            assertThat(responseEntity.getBody().getId()).isEqualTo(event.getId());
            assertThat(responseEntity.getBody().getEventType()).isEqualTo("ActivityStartedEvent");
            assertThat(responseEntity.getBody().getExecutionId()).isEqualTo("2");
            assertThat(responseEntity.getBody().getProcessDefinitionId()).isEqualTo("3");
            assertThat(responseEntity.getBody().getProcessInstanceId()).isEqualTo("4");
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
            ResponseEntity<PagedResources<ProcessEngineEventDocument>> eventsPagedResources = eventsRestTemplate.executeFindAll();
            assertThat(eventsPagedResources.getBody()).isNotEmpty();
            ProcessEngineEventDocument event = eventsPagedResources.getBody().iterator().next();

            //when
            assertThat(event).isInstanceOf(ActivityStartedEventDocument.class);
            assertThat(event.getEventType()).isEqualTo("ActivityStartedEvent");
            assertThat(event.getExecutionId()).isEqualTo("2");
            assertThat(event.getProcessDefinitionId()).isEqualTo("3");
            assertThat(event.getProcessInstanceId()).isEqualTo("4");
        });
    }

}