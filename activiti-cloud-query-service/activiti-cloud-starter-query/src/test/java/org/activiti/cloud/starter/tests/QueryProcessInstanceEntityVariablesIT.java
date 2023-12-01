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

import static org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus.RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.VariableEventContainedBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class QueryProcessInstanceEntityVariablesIT {

    private static final String VARIABLES_URL = "/v1/process-instances/{processInstanceId}/variables";
    private static final ParameterizedTypeReference<PagedModel<ProcessVariableEntity>> PAGED_VARIABLE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<ProcessVariableEntity>>() {};

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private BPMNSequenceFlowRepository sequenceFlowRepository;

    @Autowired
    private BPMNActivityRepository activityRepository;

    @Autowired
    private TaskRepository taskRepository;

    private EventsAggregator eventsAggregator;

    private VariableEventContainedBuilder variableEventContainedBuilder;

    @Autowired
    private MyProducer myProducer;

    @Autowired
    private SubscribableChannel errorChannel;

    private ProcessInstanceEventContainedBuilder processInstanceEventContainedBuilder;

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(myProducer).errorChannel(errorChannel);
        processInstanceEventContainedBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
        variableEventContainedBuilder = new VariableEventContainedBuilder(eventsAggregator);
    }

    @AfterEach
    public void tearDown() {
        taskRepository.deleteAll();
        variableRepository.deleteAll();
        activityRepository.deleteAll();
        sequenceFlowRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldRetrieveAllProcessVariable() {
        //given
        var runningProcessInstance = processInstanceEventContainedBuilder.aRunningProcessInstance(
            "process with variables"
        );

        variableEventContainedBuilder
            .aCreatedVariable("varCreated", "v1", "string")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .anUpdatedVariable("varUpdated", "v2-up", "beforeUpdateValue", "string")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aDeletedVariable("varDeleted", "v1", "string")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessVariableEntity>> responseEntity = testRestTemplate.exchange(
                    VARIABLES_URL,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_VARIABLE_RESPONSE_TYPE,
                    runningProcessInstance.getId()
                );

                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody().getContent())
                    .extracting(
                        ProcessVariableEntity::getName,
                        ProcessVariableEntity::getValue,
                        ProcessVariableEntity::getMarkedAsDeleted
                    )
                    .containsExactly(tuple("varCreated", "v1", false), tuple("varUpdated", "v2-up", false));
            });
    }

    @Test
    public void shouldSupportIntegerVariables() {
        //given
        var runningProcessInstance = processInstanceEventContainedBuilder.aRunningProcessInstance(
            "process with variables"
        );

        variableEventContainedBuilder
            .aCreatedVariable("intVar", 10, "integer")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessVariableEntity>> responseEntity = testRestTemplate.exchange(
                    VARIABLES_URL,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_VARIABLE_RESPONSE_TYPE,
                    runningProcessInstance.getId()
                );

                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody().getContent())
                    .extracting(ProcessVariableEntity::getName, ProcessVariableEntity::getValue)
                    .containsExactly(tuple("intVar", 10));
            });
    }

    @Test
    public void shouldFilterOnVariableName() {
        //given
        var runningProcessInstance = processInstanceEventContainedBuilder.aRunningProcessInstance(
            "process with variables"
        );

        variableEventContainedBuilder
            .aCreatedVariable("var1", "v1", "string")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariable("var2", "v2", "string")
            .onProcessInstance(runningProcessInstance);

        variableEventContainedBuilder
            .aCreatedVariable("var3", "v3", "string")
            .onProcessInstance(runningProcessInstance);

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<ProcessVariableEntity>> responseEntity = testRestTemplate.exchange(
                    VARIABLES_URL + "?name={varName}",
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_VARIABLE_RESPONSE_TYPE,
                    runningProcessInstance.getId(),
                    "var2"
                );

                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody().getContent())
                    .extracting(ProcessVariableEntity::getName, ProcessVariableEntity::getValue)
                    .containsExactly(tuple("var2", "v2"));
            });
    }

    @Test
    void should_handleDuplicateSimpleProcessInstanceWithVariablesEvents() {
        // given
        var simpleProcessInstance = processInstanceEventContainedBuilder.startSimpleProcessInstance(
            "sampleDefinitionId"
        );

        variableEventContainedBuilder
            .aCreatedVariable("varCreated", "v1", "string")
            .onProcessInstance(simpleProcessInstance);

        variableEventContainedBuilder
            .anUpdatedVariable("varUpdated", "v2-up", "beforeUpdateValue", "string")
            .onProcessInstance(simpleProcessInstance);

        variableEventContainedBuilder
            .aDeletedVariable("varDeleted", "v1", "string")
            .onProcessInstance(simpleProcessInstance);

        // when
        assertThat(eventsAggregator.getException()).isNull();
        var sentEvents = eventsAggregator.sendAll();

        // then
        assertThat(processInstanceRepository.findById(simpleProcessInstance.getId()))
            .isNotEmpty()
            .get()
            .extracting(ProcessInstanceEntity::getStatus)
            .isEqualTo(RUNNING);

        assertThat(variableRepository.findAll())
            .filteredOn(it -> simpleProcessInstance.getId().equals(it.getProcessInstanceId()))
            .extracting(ProcessVariableEntity::getName, ProcessVariableEntity::getValue)
            .containsExactly(tuple("varCreated", "v1"), tuple("varUpdated", "v2-up"));

        // and when duplicates are sent
        eventsAggregator.addEvents(sentEvents).sendAll();

        // and then
        assertThat(eventsAggregator.getException()).isNull();
    }
}
