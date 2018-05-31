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

package org.activiti.cloud.starter.tests;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.Variable;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.activiti.cloud.starter.tests.CoreTaskBuilder.aTask;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessCreatedEvent;
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
@TestPropertySource("classpath:application-test-admin.properties")
public class QueryAdminVariablesIT {

    private static final String VARIABLES_URL = "/admin/v1/variables?processInstanceId={processInstanceId}";
    private static final String TASK_VARIABLES_URL = "/admin/v1/variables?taskId={taskId}";
    private static final ParameterizedTypeReference<PagedResources<Variable>> PAGED_VARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<Variable>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private MyProducer producer;

    @After
    public void tearDown() throws Exception {
        variableRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldRetrieveAllProcessVariable() throws Exception {

        //given
        String processInstanceId = "120";
        long timestamp = System.currentTimeMillis();

        List<ProcessEngineEvent> createProcess = new ArrayList<ProcessEngineEvent>();
        createProcess.addAll(Arrays.asList(aProcessCreatedEvent(timestamp,
                "110",
                "defId",
                processInstanceId)));
        createProcess.addAll(Arrays.asList(aProcessStartedEvent(timestamp,
                "110",
                "defId",
                processInstanceId)));

        List<ProcessEngineEvent> createAndUpdateVariable = new ArrayList<ProcessEngineEvent>();
        createAndUpdateVariable.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withProcessInstanceId(processInstanceId)
                .withVariableName("varUpdated")
                .withVariableValue("v2")
                .withVariableType("string")
                .build()));
        createAndUpdateVariable.addAll(Arrays.asList(aVariableUpdatedEvent(timestamp)
                .withProcessInstanceId(processInstanceId)
                .withVariableName("varUpdated")
                .withVariableValue("v2-up")
                .withVariableType("string")
                .build()));

        // a variable created and deleted
        List<ProcessEngineEvent> createAndDeleteVariable = new ArrayList<ProcessEngineEvent>();
        createAndDeleteVariable.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withVariableName("varDeleted")
                .withVariableValue("v1")
                .withVariableType("string")
                .withProcessInstanceId(processInstanceId)
                .build()));
        createAndDeleteVariable.addAll(Arrays.asList(aVariableDeletedEvent(timestamp)
                .withProcessInstanceId(processInstanceId)
                .withVariableName("varDeleted")
                .withVariableValue("v3")
                .withVariableType("string")
                .build()));

        List<ProcessEngineEvent> createUpdateAndDeleteSequences = new ArrayList<ProcessEngineEvent>();
        createUpdateAndDeleteSequences.addAll(createProcess);
        createUpdateAndDeleteSequences.addAll(createAndUpdateVariable);
        createUpdateAndDeleteSequences.addAll(createAndDeleteVariable);

        producer.send(createUpdateAndDeleteSequences.toArray(new ProcessEngineEvent[]{}));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Variable>> responseEntity = testRestTemplate.exchange(VARIABLES_URL,
                                                                                                HttpMethod.GET,
                    getHeaderEntity(),
                                                                                                PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                processInstanceId);

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            Variable::getName,
                            Variable::getValue)
                    .containsExactly(
                            tuple(
                                    "varUpdated",
                                    "v2-up"),
                            tuple(
                                    "varDeleted",
                                    "v1")
                    );
        });
    }


    @Test
    public void shouldRetrieveAllTaskVariables() throws Exception {
        //given
        long timestamp = System.currentTimeMillis();
        String TASK_ID = "131";
        String PROCESS_INSTANCE_ID = "116";

        List<ProcessEngineEvent> createProcess = new ArrayList<ProcessEngineEvent>();
        createProcess.addAll(Arrays.asList(aProcessCreatedEvent(System.currentTimeMillis(),
                "110",
                "defId",
                PROCESS_INSTANCE_ID)));
        createProcess.addAll(Arrays.asList(aProcessStartedEvent(System.currentTimeMillis(),
                "110",
                "defId",
                PROCESS_INSTANCE_ID)));

        List<ProcessEngineEvent> createTask = new ArrayList<ProcessEngineEvent>();
        createTask.addAll(Arrays.asList(aTaskCreatedEvent(System.currentTimeMillis(),
                aTask()
                        .withId(TASK_ID)
                        .withName("Created task")
                        .build(),
                PROCESS_INSTANCE_ID)));

        List<ProcessEngineEvent> createAndUpdateVariables = new ArrayList<ProcessEngineEvent>();
        // a variable created
        createAndUpdateVariables.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withTaskId(TASK_ID)
                .withProcessInstanceId(PROCESS_INSTANCE_ID)
                .withVariableName("varCreated")
                .withVariableValue("v1")
                .withVariableType("string")
                .build()));
        // a variable created and updated
        createAndUpdateVariables.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withTaskId(TASK_ID)
                .withProcessInstanceId(PROCESS_INSTANCE_ID)
                .withVariableName("varUpdated")
                .withVariableValue("v2")
                .withVariableType("string")
                .build()));
        createAndUpdateVariables.addAll(Arrays.asList(aVariableUpdatedEvent(timestamp)
                .withTaskId(TASK_ID)
                .withProcessInstanceId(PROCESS_INSTANCE_ID)
                .withVariableName("varUpdated")
                .withVariableValue("v2-up")
                .withVariableType("string")
                .build()));

        List<ProcessEngineEvent> createProcessTaskAndVariables = new ArrayList<ProcessEngineEvent>();
        createProcessTaskAndVariables.addAll(createProcess);
        createProcessTaskAndVariables.addAll(createTask);
        createProcessTaskAndVariables.addAll(createAndUpdateVariables);

        producer.send(createProcessTaskAndVariables.toArray(new ProcessEngineEvent[]{}));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<Variable>> responseEntity = testRestTemplate.exchange(TASK_VARIABLES_URL,
                    HttpMethod.GET,
                    getHeaderEntity(),
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
        String processInstanceId = "121";
        long timestamp = System.currentTimeMillis();

        List<ProcessEngineEvent> createProcess = new ArrayList<ProcessEngineEvent>();
        createProcess.addAll(Arrays.asList(aProcessCreatedEvent(timestamp,
                "111",
                "defId",
                processInstanceId)));
        createProcess.addAll(Arrays.asList(aProcessStartedEvent(timestamp,
                "111",
                "defId",
                processInstanceId)));

        List<ProcessEngineEvent> createVariables = new ArrayList<>();
        createVariables.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withProcessInstanceId(processInstanceId)
                .withVariableName("var1")
                .withVariableValue("v1")
                .withVariableType("string")
                .build()));
        createVariables.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withProcessInstanceId(processInstanceId)
                .withVariableName("var2")
                .withVariableValue("v2")
                .withVariableType("string")
                .build()));
        createVariables.addAll(Arrays.asList(aVariableCreatedEvent(timestamp)
                .withVariableName("var3")
                .withVariableValue("v3")
                .withVariableType("string")
                .withProcessInstanceId(processInstanceId)
                .build()));

        List<ProcessEngineEvent> createProcessAndVariables = new ArrayList<>();
        createProcessAndVariables.addAll(createProcess);
        createProcessAndVariables.addAll(createVariables);

        producer.send(createProcessAndVariables.toArray(new ProcessEngineEvent[]{}));

        //awaitility doesn't seem to like this test (just fails with a timeout but doesn't actually wait), not sure why - having to use sleep
        Thread.sleep(300);

            //when
            ResponseEntity<PagedResources<Variable>> responseEntity = testRestTemplate.exchange(VARIABLES_URL + "&name={name}",
                                                                                                HttpMethod.GET,
                    getHeaderEntity(),
                                                                                                PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                processInstanceId,
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

    }


    private HttpEntity getHeaderEntity(){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", keycloakTokenProducer.getTokenString());
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        return entity;
    }

}
