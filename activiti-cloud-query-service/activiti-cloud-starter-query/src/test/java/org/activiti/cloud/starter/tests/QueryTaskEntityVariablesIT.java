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

import java.util.Date;
import java.util.UUID;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
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
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryTaskEntityVariablesIT {

    private static final String VARIABLES_URL = "/v1/tasks/{taskId}/variables";
    private static final String ADMIN_VARIABLES_URL = "/admin/v1/tasks/{taskId}/variables";

    private static final ParameterizedTypeReference<PagedModel<TaskVariableEntity>> PAGED_VARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<TaskVariableEntity>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TaskVariableRepository variableRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    private EventsAggregator eventsAggregator;

    private ProcessInstanceEventContainedBuilder processInstanceEventContainedBuilder;

    private TaskEventContainedBuilder taskEventContainedBuilder;

    private VariableEventContainedBuilder variableEventContainedBuilder;

    private Task task;

    private Task standAloneTask;

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceEventContainedBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        taskEventContainedBuilder = new TaskEventContainedBuilder(eventsAggregator);
        variableEventContainedBuilder = new VariableEventContainedBuilder(eventsAggregator);

        ProcessInstance runningProcessInstance = processInstanceEventContainedBuilder.aRunningProcessInstance("Process with variables");

        task = taskEventContainedBuilder.aCreatedTask("Created task",
                                                      runningProcessInstance);
        standAloneTask = taskEventContainedBuilder.aCreatedStandaloneTaskWithParent("StandAlone task");

    }

    @AfterEach
    public void tearDown() {
        variableRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldRetrieveAllTaskVariables() {
        //given

        variableEventContainedBuilder.aCreatedVariable("varCreated",
                                                       "v1",
                                                       "string")
                .onTask(task);

        variableEventContainedBuilder.anUpdatedVariable("varUpdated",
                                                        "v2-up",
                                                        "string")
                .onTask(task);

        variableEventContainedBuilder.aDeletedVariable("varDeleted",
                                                       "v1",
                                                       "string")
                .onTask(task);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple(
                                    "varCreated",
                                    "v1",
                                    false),
                            tuple(
                                    "varUpdated",
                                    "v2-up",
                                    false)
                    );
        });

    }

    @Test
    public void shouldFilterOnVariableName() {
        //given
        variableEventContainedBuilder.aCreatedVariable("var1",
                                                       "v1",
                                                       "string")
                .onTask(task);
        variableEventContainedBuilder.aCreatedVariable("var2",
                                                       "v2",
                                                       "string")
                .onTask(task);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = testRestTemplate.exchange(VARIABLES_URL + "?name={varName}",
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_VARIABLE_RESPONSE_TYPE,
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


    @Test
    public void shouldNotSeeAdminVariables() {

        //when
        ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = testRestTemplate.exchange(ADMIN_VARIABLES_URL,
                             HttpMethod.GET,
                             keycloakTokenProducer.entityWithAuthorizationHeader(),
                             PAGED_VARIABLE_RESPONSE_TYPE,
                             task.getId());
         //then
         assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void shouldRetrieveVariablesFromStandAloneTask(){
        //given

        variableEventContainedBuilder.aCreatedVariable("varCreated",
                "v1",
                "string")
                .onTask(standAloneTask);

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(standAloneTask.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getType)
                    .contains(

                            tuple("varCreated",
                                            "v1",
                                            "string"
                            ));

        });
    }

    @Test
    public void shouldGetTaskVariablesAfterTaskCompleted() {
        //given
        VariableInstanceImpl<String> var = new VariableInstanceImpl<>("var",
                                                              "string",
                                                              "value",
                                                              null);
        var.setTaskId(task.getId());

        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(var));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple( "var",
                                   "value",
                                   false)
                    );
        });

        ((TaskImpl)task).setStatus(Task.TaskStatus.COMPLETED);
        eventsAggregator.addEvents(new CloudTaskCompletedEventImpl(UUID.randomUUID().toString(), new Date().getTime(), task));
        eventsAggregator.sendAll();
        producer.send(new CloudVariableDeletedEventImpl(var));

        await().untilAsserted(() -> {
            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple( "var",
                                   "value",
                                   false)
                    );
        });

    }


    @Test
    public void shouldNotCreateTaskVariableWithSameName() {
        //given
        VariableInstanceImpl<String> var = new VariableInstanceImpl<>("varCreated",
                                                              "string",
                                                              "value",
                                                              null);
        var.setTaskId(task.getId());
        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(var));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple( "varCreated",
                                   "value",
                                   false)
                    );
        });

        var = new VariableInstanceImpl<>("varCreated",
                "string",
                "new value",
                null);
        var.setTaskId(task.getId());
        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(var));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
          //when
          ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity1 = getTaskVariables(task.getId());

          //then
          assertThat(responseEntity1.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(responseEntity1.getBody().getContent())
                  .extracting(
                          TaskVariableEntity::getName,
                          TaskVariableEntity::getValue,
                          TaskVariableEntity::getMarkedAsDeleted)
                  .containsExactly(
                          tuple( "varCreated",
                                 "value",
                                 false)
                  );
        });

    }


    @Test
    public void shouldReCreateVariableAfterItWasDeleted() {
        //given
        VariableInstanceImpl<String> var = new VariableInstanceImpl<>("var",
                                                              "string",
                                                              "value",
                                                              null);
        var.setTaskId(task.getId());

        eventsAggregator.addEvents(new CloudVariableCreatedEventImpl(var));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple( "var",
                                   "value",
                                   false)
                    );
        });

        producer.send(new CloudVariableDeletedEventImpl(var));

        await().untilAsserted(() -> {
            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent().size()).isEqualTo(0);
        });

        //Create a variable with the same name
        var = new VariableInstanceImpl<>("var",
                "string",
                "new value",
                null);
        var.setTaskId(task.getId());
        producer.send(new CloudVariableCreatedEventImpl(var));

        await().untilAsserted(() -> {
            //when
            ResponseEntity<PagedModel<TaskVariableEntity>> responseEntity = getTaskVariables(task.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            TaskVariableEntity::getName,
                            TaskVariableEntity::getValue,
                            TaskVariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple( "var",
                                   "new value",
                                   false)
                    );
        });

    }


    public  ResponseEntity<PagedModel<TaskVariableEntity>> getTaskVariables(String taskId) {
        return testRestTemplate.exchange(VARIABLES_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_VARIABLE_RESPONSE_TYPE,
                                         taskId);
    }

}
