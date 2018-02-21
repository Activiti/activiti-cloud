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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessCreatedEvent;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessStartedEvent;
import static org.activiti.cloud.starters.test.builder.VariableCreatedEventBuilder.aVariableCreatedEvent;
import static org.activiti.cloud.starters.test.builder.VariableDeletedEventBuilder.aVariableDeletedEvent;
import static org.activiti.cloud.starters.test.builder.VariableUpdatedEventBuilder.aVariableUpdatedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
public class QueryProcessInstanceVariablesIT {

    private static final String VARIABLES_URL = "/v1/variables?processInstanceId={processInstanceId}";
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
        String processInstanceId = "20";
        long timestamp = System.currentTimeMillis();

        producer.send(aProcessCreatedEvent(timestamp,
                                           "10",
                                           "defId",
                                           processInstanceId));
        producer.send(aProcessStartedEvent(timestamp,
                                           "10",
                                           "defId",
                                           processInstanceId));
        // a variable created
        producer.send(aVariableCreatedEvent(timestamp)
                              .withProcessInstanceId(processInstanceId)
                              .withVariableName("varCreated")
                              .withVariableValue("v1")
                              .withVariableType("string")
                              .build());

        // a variable created and updated
        producer.send(aVariableCreatedEvent(timestamp)
                              .withProcessInstanceId(processInstanceId)
                              .withVariableName("varUpdated")
                              .withVariableValue("v2")
                              .withVariableType("string")
                              .build());
        producer.send(aVariableUpdatedEvent(timestamp)
                              .withProcessInstanceId(processInstanceId)
                              .withVariableName("varUpdated")
                              .withVariableValue("v2-up")
                              .withVariableType("string")
                              .build());

        // a variable created and deleted
        producer.send(aVariableCreatedEvent(timestamp)
                              .withVariableName("varDeleted")
                              .withVariableValue("v1")
                              .withVariableType("string")
                              .withProcessInstanceId(processInstanceId)
                              .build());
        producer.send(aVariableDeletedEvent(timestamp)
                              .withProcessInstanceId(processInstanceId)
                              .withVariableName("varDeleted")
                              .withVariableType("string")
                              .build());

        await().untilAsserted(() -> {

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
        String processInstanceId = "20";
        long timestamp = System.currentTimeMillis();

        producer.send(aProcessCreatedEvent(timestamp,
                                           "10",
                                           "defId",
                                           processInstanceId));
        producer.send(aProcessStartedEvent(timestamp,
                                           "10",
                                           "defId",
                                           processInstanceId));
        producer.send(aVariableCreatedEvent(timestamp)
                              .withProcessInstanceId(processInstanceId)
                              .withVariableName("var1")
                              .withVariableValue("v1")
                              .withVariableType("string")
                              .build());

        producer.send(aVariableCreatedEvent(timestamp)
                              .withProcessInstanceId(processInstanceId)
                              .withVariableName("var2")
                              .withVariableValue("v2")
                              .withVariableType("string")
                              .build());

        producer.send(aVariableCreatedEvent(timestamp)
                              .withVariableName("var3")
                              .withVariableValue("v3")
                              .withVariableType("string")
                              .withProcessInstanceId(processInstanceId)
                              .build());

        await().untilAsserted(() -> {

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
        });
    }


    private HttpEntity getHeaderEntity(){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", keycloakTokenProducer.getTokenString());
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        return entity;
    }
}
