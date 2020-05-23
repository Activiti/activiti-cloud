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
package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.TaskEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.VariableEventContainedBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test-admin.properties")
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryAdminVariablesIT {

    private static final String ADMIN_PROCESS_VARIABLES_URL = "/admin/v1/process-instances/{processInstanceId}/variables";
    private static final String ADMIN_TASK_VARIABLES_URL = "/admin/v1/tasks/{taskId}/variables";

    private static final ParameterizedTypeReference<PagedModel<ProcessVariableEntity>> PAGED_PROCESSVARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<ProcessVariableEntity>>() {
    };
    private static final ParameterizedTypeReference<PagedModel<TaskVariableEntity>> PAGED_TASKVARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<TaskVariableEntity>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private VariableRepository processVariableRepository;

    @Autowired
    private TaskVariableRepository taskVariableRepository;

    @Autowired
    private MyProducer producer;

    private ProcessInstance runningProcessInstance;

    private EventsAggregator eventsAggregator;
    private VariableEventContainedBuilder variableEventContainedBuilder;
    private TaskEventContainedBuilder taskEventContainedBuilder;

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        ProcessInstanceEventContainedBuilder processInstanceEventContainedBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        taskEventContainedBuilder = new TaskEventContainedBuilder(eventsAggregator);
        variableEventContainedBuilder = new VariableEventContainedBuilder(eventsAggregator);

        runningProcessInstance = processInstanceEventContainedBuilder.aRunningProcessInstance("Process with variables");
    }

    @AfterEach
    public void tearDown() {
        processVariableRepository.deleteAll();
        taskVariableRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldRetrieveAllProcessVariable() {
        //given
        variableEventContainedBuilder.anUpdatedVariable("varUpdated",
                                                        "v2-up",
                                                        "string")
                .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder.aDeletedVariable("varDeleted",
                                                       "v1",
                                                       "string")
                .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<ProcessVariableEntity>> responseEntity = testRestTemplate.exchange(ADMIN_PROCESS_VARIABLES_URL,
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_PROCESSVARIABLE_RESPONSE_TYPE,
                                                                                                      runningProcessInstance.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            ProcessVariableEntity::getName,
                            ProcessVariableEntity::getValue,
                            ProcessVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple(
                                    "varUpdated",
                                    "v2-up",
                                    false)
                    );
        });
    }

    @Test
    public void shouldRetrieveAllTaskVariables() {
        //given
        Task task = taskEventContainedBuilder.aCreatedTask("Created task",
                                                           runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("varCreated",
                                                       "v1",
                                                       "string")
                .onTask(task);
        variableEventContainedBuilder.anUpdatedVariable("varUpdated",
                                                        "v2-up",
                                                        "string")
                .onTask(task);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = testRestTemplate.exchange(ADMIN_TASK_VARIABLES_URL,
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_TASKVARIABLE_RESPONSE_TYPE,
                                                                                                      task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue)
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
    public void shouldFilterOnProcessVariableName() {

        //given
        variableEventContainedBuilder.aCreatedVariable("var1",
                                                       "v1",
                                                       "string")
                .onProcessInstance(runningProcessInstance);
        variableEventContainedBuilder.aCreatedVariable("var2",
                                                       "v2",
                                                       "string")
                .onProcessInstance(runningProcessInstance);
        variableEventContainedBuilder.aCreatedVariable("var3",
                                                       "v3",
                                                       "string")
                .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<ProcessVariableEntity>> responseEntity = testRestTemplate.exchange(ADMIN_PROCESS_VARIABLES_URL +  "?name={varName}",
                    HttpMethod.GET,
                    keycloakTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_PROCESSVARIABLE_RESPONSE_TYPE,
                    runningProcessInstance.getId(),
                    "var2");

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            ProcessVariableEntity::getName,
                            ProcessVariableEntity::getValue)
                    .containsExactly(
                            tuple("var2",
                                    "v2")
                    );
        });
    }

    @Test
    public void shouldFilterOnTaskVariableName() {

        //given
        Task task = taskEventContainedBuilder.aCreatedTask("Created task",
                                                           runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("var1",
                                                       "v1",
                                                       "string")
                .onTask(task);
        variableEventContainedBuilder.aCreatedVariable("var2",
                                                       "v2",
                                                       "string")
                .onTask(task);
        variableEventContainedBuilder.aCreatedVariable("var3",
                                                       "v3",
                                                       "string")
                .onTask(task);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = testRestTemplate.exchange(ADMIN_TASK_VARIABLES_URL + "?name={varName}",
                                                                                                          HttpMethod.GET,
                                                                                                          keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                          PAGED_TASKVARIABLE_RESPONSE_TYPE,
                                                                                                          task.getId(),
                                                                                                          "var2");
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue)
                    .containsExactly(
                            tuple("var2",
                                    "v2")
                    );
        });
    }

    //Test a case when a processInstance and a task have variable with the same name
    @Test
    public void shouldFilterOnProcessAndTaskVariableName() {

        //given
        variableEventContainedBuilder.aCreatedVariable("var1",
                                                       "pv1",
                                                       "string")
                .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder.aCreatedVariable("var2",
                                                       "pv2",
                                                       "string")
                .onProcessInstance(runningProcessInstance);

        Task task = taskEventContainedBuilder.aCreatedTask("Created task",
                                                           runningProcessInstance);

        //One of task variables has same name like processInstance variable
        variableEventContainedBuilder.aCreatedVariable("var1",
                                                       "tv1",
                                                       "string")
                .onTask(task);
        variableEventContainedBuilder.aCreatedVariable("var2",
                                                       "tv2",
                                                       "string")
                .onTask(task);
        variableEventContainedBuilder.aCreatedVariable("var3",
                                                       "v3",
                                                       "string")
                .onTask(task);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> taskResponseEntity = testRestTemplate.exchange(ADMIN_TASK_VARIABLES_URL + "?name={varName}",
                                                                                                          HttpMethod.GET,
                                                                                                          keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                          PAGED_TASKVARIABLE_RESPONSE_TYPE,
                                                                                                          task.getId(),
                                                                                                          "var1");
            //then
            assertThat(taskResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(taskResponseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue)
                    .containsExactly(
                            tuple("var1",
                                   "tv1")
                    );

            //when
            ResponseEntity<PagedModel<ProcessVariableEntity>> processResponseEntity = testRestTemplate.exchange(ADMIN_PROCESS_VARIABLES_URL +  "?name={varName}",
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_PROCESSVARIABLE_RESPONSE_TYPE,
                                                                                                      runningProcessInstance.getId(),
                                                                                                      "var1");

            //then
            assertThat(processResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(processResponseEntity.getBody().getContent())
                    .extracting(
                            ProcessVariableEntity::getName,
                            ProcessVariableEntity::getValue)
                    .containsExactly(
                            tuple("var1",
                                   "pv1")
                    );
        });
    }
}
