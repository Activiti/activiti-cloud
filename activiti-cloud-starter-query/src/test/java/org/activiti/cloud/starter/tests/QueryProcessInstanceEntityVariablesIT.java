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

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.VariableEventContainedBuilder;
import org.activiti.runtime.api.model.ProcessInstance;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class QueryProcessInstanceEntityVariablesIT {

    private static final String VARIABLES_URL = "/v1/process-instances/{processInstanceId}/variables";
    private static final ParameterizedTypeReference<PagedResources<VariableEntity>> PAGED_VARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<VariableEntity>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    private EventsAggregator eventsAggregator;

    private VariableEventContainedBuilder variableEventContainedBuilder;

    @Autowired
    private MyProducer myProducer;

    private ProcessInstance runningProcessInstance;

    @Before
    public void setUp() {
        eventsAggregator = new EventsAggregator(myProducer);
        ProcessInstanceEventContainedBuilder processInstanceEventContainedBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        variableEventContainedBuilder = new VariableEventContainedBuilder(eventsAggregator);

        runningProcessInstance = processInstanceEventContainedBuilder.aRunningProcessInstance("process with variables");
    }

    @After
    public void tearDown() {
        variableRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldRetrieveAllProcessVariable() {
        //given
        variableEventContainedBuilder.aCreatedVariable("varCreated",
                                                       "v1",
                                                       "string")
                .onProcessInstance(runningProcessInstance);

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
            ResponseEntity<PagedResources<VariableEntity>> responseEntity = testRestTemplate.exchange(VARIABLES_URL,
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                      runningProcessInstance.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            VariableEntity::getName,
                            VariableEntity::getValue,
                            VariableEntity::getMarkedAsDeleted)
                    .containsExactly(
                            tuple(
                                    "varCreated",
                                    "v1",
                                    false),
                            tuple(
                                    "varUpdated",
                                    "v2-up",
                                    false),
                            tuple(// Variables deleted should be here, they are soft deleted
                                  "varDeleted",
                                  "v1",
                                  true)
                    );
        });
    }

    @Test
    public void shouldSupportIntegerVariables() {
        //given
        variableEventContainedBuilder.aCreatedVariable("intVar",
                                                       10,
                                                       "integer")
                .onProcessInstance(runningProcessInstance);


        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<VariableEntity>> responseEntity = testRestTemplate.exchange(VARIABLES_URL,
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                      runningProcessInstance.getId());

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            VariableEntity::getName,
                            VariableEntity::getValue)
                    .containsExactly(
                            tuple(
                                    "intVar",
                                    10)
                    );
        });
    }

    @Test
    public void shouldFilterOnVariableName() {
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
            ResponseEntity<PagedResources<VariableEntity>> responseEntity = testRestTemplate.exchange(VARIABLES_URL + "?name={varName}",
                                                                                                      HttpMethod.GET,
                                                                                                      keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                      PAGED_VARIABLE_RESPONSE_TYPE,
                                                                                                      runningProcessInstance.getId(),
                                                                                                      "var2");

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().getContent())
                    .extracting(
                            VariableEntity::getName,
                            VariableEntity::getValue)
                    .containsExactly(
                            tuple("var2",
                                  "v2")
                    );
        });
    }
}
