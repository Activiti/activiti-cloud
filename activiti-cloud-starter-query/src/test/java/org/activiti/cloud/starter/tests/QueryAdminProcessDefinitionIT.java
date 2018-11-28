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

package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.UUID;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
public class QueryAdminProcessDefinitionIT {

    private static final String PROC_DEFINITIONS_URL = "/admin/v1/process-definitions";
    private static final ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> PAGED_PROCESS_DEFINITION_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private MyProducer producer;

    @Before
    public void setUp() {
        keycloakTokenProducer.setKeycloakTestUser("hradmin");
    }

    @After
    public void tearDown() {
        processDefinitionRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableProcessDefinitions() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());
        firstProcessDefinition.setKey("myFirstProcess");
        firstProcessDefinition.setName("My First Process");

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());
        secondProcessDefinition.setKey("mySecondProcess");
        secondProcessDefinition.setName("My second Process");
        producer.send(new CloudProcessDeployedEventImpl(firstProcessDefinition),
                      new CloudProcessDeployedEventImpl(secondProcessDefinition));

        //when
        ResponseEntity<PagedResources<CloudProcessDefinition>> responseEntity = executeRequestGetProcDefinitions();

        //then
        assertThat(responseEntity.getBody())
                .isNotNull()
                .extracting(ProcessDefinition::getId,
                            ProcessDefinition::getName,
                            ProcessDefinition::getKey)
                .containsExactly(tuple(firstProcessDefinition.getId(),
                                       "My First Process",
                                       "myFirstProcess"),
                                 tuple(secondProcessDefinition.getId(),
                                       "My second Process",
                                       "mySecondProcess"));
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> executeRequestGetProcDefinitions() {
        ResponseEntity<PagedResources<CloudProcessDefinition>> responseEntity = testRestTemplate.exchange(PROC_DEFINITIONS_URL,
                                                                                                          HttpMethod.GET,
                                                                                                          keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                          PAGED_PROCESS_DEFINITION_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> executeRequestGetProcDefinitionsFilteredOnKey(String key) {
        ResponseEntity<PagedResources<CloudProcessDefinition>> responseEntity = testRestTemplate.exchange(PROC_DEFINITIONS_URL + "?key={key}",
                                                                                                          HttpMethod.GET,
                                                                                                          keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                          PAGED_PROCESS_DEFINITION_RESPONSE_TYPE,
                                                                                                          key);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    @Test
    public void shouldFilterOnProcessKey() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());
        firstProcessDefinition.setKey("myFirstProcess");
        firstProcessDefinition.setName("My First Process");

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());
        secondProcessDefinition.setKey("mySecondProcess");
        secondProcessDefinition.setName("My second Process");
        producer.send(new CloudProcessDeployedEventImpl(firstProcessDefinition),
                      new CloudProcessDeployedEventImpl(secondProcessDefinition));

        //when
        ResponseEntity<PagedResources<CloudProcessDefinition>> responseEntity = executeRequestGetProcDefinitionsFilteredOnKey("mySecondProcess");

        //then
        assertThat(responseEntity.getBody())
                .isNotNull()
                .extracting(ProcessDefinition::getId,
                            ProcessDefinition::getName,
                            ProcessDefinition::getKey)
                .containsExactly(tuple(secondProcessDefinition.getId(),
                                       "My second Process",
                                       "mySecondProcess"));
    }

    @Test
    public void shouldUpdateDefinitionOnDuplicate() {
        //given
        ProcessDefinitionImpl initialProcessDefinition = new ProcessDefinitionImpl();
        String processDefinitionId = UUID.randomUUID().toString();
        initialProcessDefinition.setId(processDefinitionId);
        initialProcessDefinition.setKey("myProcess");
        initialProcessDefinition.setName("My Process");

        ProcessDefinitionImpl duplicatedProcessDefinition = new ProcessDefinitionImpl();
        duplicatedProcessDefinition.setId(processDefinitionId); //it has the same id as the first one, so it will be considered as duplicate
        duplicatedProcessDefinition.setKey("myProcessUpdated");
        duplicatedProcessDefinition.setName("My Process updated");
        duplicatedProcessDefinition.setDescription("Updated description");
        producer.send(new CloudProcessDeployedEventImpl(initialProcessDefinition),
                      new CloudProcessDeployedEventImpl(duplicatedProcessDefinition));

        //when
        ResponseEntity<PagedResources<CloudProcessDefinition>> responseEntity = executeRequestGetProcDefinitions();

        //then
        assertThat(responseEntity.getBody())
                .isNotNull()
                .extracting(ProcessDefinition::getId,
                            ProcessDefinition::getKey,
                            ProcessDefinition::getName,
                            ProcessDefinition::getDescription)
                .containsExactly(tuple(initialProcessDefinition.getId(),
                                       "myProcessUpdated",
                                       "My Process updated",
                                       "Updated description"));
    }
}
