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

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.Variable;
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.services.query.CoreTaskBuilder.aTask;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessStartedEvent;
import static org.activiti.cloud.starters.test.MockTaskEvent.aTaskCreatedEvent;
import static org.activiti.cloud.starters.test.builder.VariableCreatedEventBuilder.aVariableCreatedEvent;
import static org.activiti.cloud.starters.test.builder.VariableDeletedEventBuilder.aVariableDeletedEvent;
import static org.activiti.cloud.starters.test.builder.VariableUpdatedEventBuilder.aVariableUpdatedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QueryTaskVariablesIT {

    private static final String VARIABLES_URL = "/v1/variables?taskId={taskId}";
    private static final ParameterizedTypeReference<PagedResources<Variable>> PAGED_VARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<Variable>>() {
    };

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    private static final String PROCESS_INSTANCE_ID = "15";
    private static final String TASK_ID = "30";

    @Before
    public void setUp() throws Exception {
        // start a process
        producer.send(aProcessStartedEvent(System.currentTimeMillis(),
                                           "10",
                                           "defId",
                                           PROCESS_INSTANCE_ID));

        producer.send(aTaskCreatedEvent(System.currentTimeMillis(),
                                        aTask()
                                                .withId(TASK_ID)
                                                .withName("Created task")
                                                .build(),
                                        PROCESS_INSTANCE_ID));
    }

    @After
    public void tearDown() throws Exception {
        variableRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldRetrieveAllTaskVariables() throws Exception {
        //given
        long timestamp = System.currentTimeMillis();

        // a variable created
        producer.send(aVariableCreatedEvent(timestamp)
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .withVariableName("varCreated")
                              .withVariableValue("v1")
                              .withVariableType("string")
                              .build());

        // a variable created and updated
        producer.send(aVariableCreatedEvent(timestamp)
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .withVariableName("varUpdated")
                              .withVariableValue("v2")
                              .withVariableType("string")
                              .build());
        producer.send(aVariableUpdatedEvent(timestamp)
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .withVariableName("varUpdated")
                              .withVariableValue("v2-up")
                              .withVariableType("string")
                              .build());

        // a variable created and deleted
        producer.send(aVariableCreatedEvent(timestamp)
                              .withVariableName("varDeleted")
                              .withVariableValue("v1")
                              .withVariableType("string")
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .build());
        producer.send(aVariableDeletedEvent(timestamp)
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .withVariableName("varDeleted")
                              .withVariableType("string")
                              .build());

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Variable>> responseEntity = testRestTemplate.exchange(VARIABLES_URL,
                                                                                                HttpMethod.GET,
                                                                                                null,
                                                                                                PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                TASK_ID);

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            Variable::getName,
                            Variable::getValue)
                    .containsExactly(
                            tuple(
                                    "varCreated",
                                    "v1"),
                            tuple(
                                    "varUpdated",
                                    "v2-up"));
        });
    }

    @Test
    public void shouldFilterOnVariableName() throws Exception {
        //given
        long timestamp = System.currentTimeMillis();
        producer.send(aVariableCreatedEvent(timestamp)
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .withVariableName("var1")
                              .withVariableValue("v1")
                              .withVariableType("string")
                              .build());

        producer.send(aVariableCreatedEvent(timestamp)
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .withVariableName("var2")
                              .withVariableValue("v2")
                              .withVariableType("string")
                              .build());

        producer.send(aVariableCreatedEvent(timestamp)
                              .withVariableName("var3")
                              .withVariableValue("v3")
                              .withVariableType("string")
                              .withTaskId(TASK_ID)
                              .withProcessInstanceId(PROCESS_INSTANCE_ID)
                              .build());

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Variable>> responseEntity = testRestTemplate.exchange(VARIABLES_URL + "&name={name}",
                                                                                                HttpMethod.GET,
                                                                                                null,
                                                                                                PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                TASK_ID,
                                                                                                "var2");

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            Variable::getName,
                            Variable::getValue)
                    .containsExactly(
                            tuple("var2",
                                  "v2")
                    );
        });
    }
}
