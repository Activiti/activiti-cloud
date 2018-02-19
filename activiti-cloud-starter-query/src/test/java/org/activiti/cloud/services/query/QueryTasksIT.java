/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.query;

import java.util.Collection;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.services.query.CoreTaskBuilder.aTask;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessCreatedEvent;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessStartedEvent;
import static org.activiti.cloud.starters.test.MockTaskEvent.aTaskAssignedEvent;
import static org.activiti.cloud.starters.test.MockTaskEvent.aTaskCompletedEvent;
import static org.activiti.cloud.starters.test.MockTaskEvent.aTaskCreatedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class QueryTasksIT {

    private static final String TASKS_URL = "/v1/tasks";
    private static final ParameterizedTypeReference<PagedResources<Task>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<Task>>() {
    };

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    private static final String PROCESS_INSTANCE_ID = "15";

    @Before
    public void setUp() throws Exception {
        // start a process
        producer.send(aProcessCreatedEvent(System.currentTimeMillis(),
                                           "10",
                                           "defId",
                                           PROCESS_INSTANCE_ID));
        producer.send(aProcessStartedEvent(System.currentTimeMillis(),
                                           "10",
                                           "defId",
                                           PROCESS_INSTANCE_ID));
    }

    @After
    public void tearDown() throws Exception {
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableTasks() throws Exception {
        //given
        // a created task
        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId("2")
                                                .withName("Created task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));

        // a assigned task
        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId("3")
                                                .withName("Assigned task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));
        producer.send(aTaskAssignedEvent(System.currentTimeMillis(),
                                         aTask()
                                                 .withId("3")
                                                 .withName("Assigned task")
                                                 .build()));
        // a completed task
        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId("4")
                                                .withName("Completed task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));
        producer.send(aTaskCompletedEvent(System.currentTimeMillis(),
                                          aTask()
                                                  .withId("4")
                                                  .withName("Completed task")
                                                  .build()));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Task>> responseEntity = executeRequestGetTasks();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .contains(tuple("2",
                                    "CREATED"),
                              tuple("3",
                                    "ASSIGNED"),
                              tuple("4",
                                    "COMPLETED"));
        });
    }

    @Test
    public void shouldFilterOnStatusTasks() throws Exception {
        //given
        // a created task
        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId("2")
                                                .withName("Created task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));

        // a assigned task
        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId("3")
                                                .withName("Assigned task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));
        producer.send(aTaskAssignedEvent(System.currentTimeMillis(),
                                         aTask()
                                                 .withId("3")
                                                 .withName("Assigned task")
                                                 .build()));
        // a completed task
        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId("4")
                                                .withName("Completed task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));
        producer.send(aTaskCompletedEvent(System.currentTimeMillis(),
                                          aTask()
                                                  .withId("4")
                                                  .withName("Completed task")
                                                  .build()));

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Task>> responseEntity = testRestTemplate.exchange(TASKS_URL + "?status={status}",
                                                                                            HttpMethod.GET,
                                                                                            null,
                                                                                            PAGED_TASKS_RESPONSE_TYPE,
                                                                                            "ASSIGNED");

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<Task> tasks = responseEntity.getBody().getContent();
            assertThat(tasks)
                    .extracting(Task::getId,
                                Task::getStatus)
                    .containsExactly(tuple("3",
                                           "ASSIGNED"));
        });
    }

    private ResponseEntity<PagedResources<Task>> executeRequestGetTasks() {
        return testRestTemplate.exchange(TASKS_URL,
                                         HttpMethod.GET,
                                         null,
                                         PAGED_TASKS_RESPONSE_TYPE);
    }
}
