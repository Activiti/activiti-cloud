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

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.test.ProcessDefinitionRestTemplate;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.util.StreamUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@Import({
    ProcessDefinitionRestTemplate.class
})
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryProcessDefinitionIT {

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private ProcessDefinitionRestTemplate restTemplate;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ProcessModelRepository processModelRepository;

    @Autowired
    private MyProducer producer;

    @BeforeEach
    public void setUp() {
        keycloakTokenProducer.setKeycloakTestUser("hruser");
    }

    @AfterEach
    public void tearDown() {
        processModelRepository.deleteAll();
        processDefinitionRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableProcessDefinitions() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());
        firstProcessDefinition.setKey("myFirstProcessKey");
        firstProcessDefinition.setName("My First Process");

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());
        secondProcessDefinition.setKey("mySecondProcess");
        secondProcessDefinition.setName("My second Process");
        producer.send(new CloudProcessDeployedEventImpl(firstProcessDefinition),
                      new CloudProcessDeployedEventImpl(secondProcessDefinition));

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = restTemplate.getProcDefinitions();

        //then
        assertThat(responseEntity.getBody())
                .isNotNull()
                .extracting(ProcessDefinition::getId,
                            ProcessDefinition::getName,
                            ProcessDefinition::getKey)
                .containsExactly(tuple(firstProcessDefinition.getId(),
                                       "My First Process",
                                       "myFirstProcessKey"),
                                 tuple(secondProcessDefinition.getId(),
                                       "My second Process",
                                       "mySecondProcess"));
    }

    @Test
    public void shouldGetAvailableProcessModels() throws Exception {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());
        firstProcessDefinition.setKey("myFirstProcessKey");
        firstProcessDefinition.setName("My First Process");

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());
        secondProcessDefinition.setKey("mySecondProcess");
        secondProcessDefinition.setName("My second Process");
        CloudProcessDeployedEventImpl firstProcessDeployedEvent = new CloudProcessDeployedEventImpl(firstProcessDefinition);
        firstProcessDeployedEvent.setProcessModelContent(StreamUtils.copyToString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("parse-for-test/processWithVariables.bpmn20.xml"),
                                                                                   StandardCharsets.UTF_8));
        CloudProcessDeployedEventImpl secondProcessDeployedEvent = new CloudProcessDeployedEventImpl(secondProcessDefinition);
        secondProcessDeployedEvent.setProcessModelContent(StreamUtils.copyToString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("parse-for-test/SimpleProcess.bpmn20.xml"),
                StandardCharsets.UTF_8));
        producer.send(firstProcessDeployedEvent,
                      secondProcessDeployedEvent);

        //when
        ResponseEntity<String> responseEntity = restTemplate.getProcDefinitionModel(firstProcessDefinition.getId());

        //then
        assertThat(responseEntity.getBody())
                .isXmlEqualToContentOf(new File("src/test/resources/parse-for-test/processWithVariables.bpmn20.xml"));

        //when
        responseEntity = restTemplate.getProcDefinitionModel(secondProcessDefinition.getId());

        //then
        assertThat(responseEntity.getBody())
                .isXmlEqualToContentOf(new File("src/test/resources/parse-for-test/SimpleProcess.bpmn20.xml"));
    }

    @Test
    public void shouldFilterOnProcessKey() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());
        firstProcessDefinition.setKey("myFirstProcessKey");
        firstProcessDefinition.setName("My First Process");

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());
        secondProcessDefinition.setKey("mySecondProcess");
        secondProcessDefinition.setName("My second Process");
        producer.send(new CloudProcessDeployedEventImpl(firstProcessDefinition),
                      new CloudProcessDeployedEventImpl(secondProcessDefinition));

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = restTemplate.getProcDefinitionsFilteredOnKey("mySecondProcess");

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
        initialProcessDefinition.setKey("myProcessKey");
        initialProcessDefinition.setName("My Process");

        ProcessDefinitionImpl duplicatedProcessDefinition = new ProcessDefinitionImpl();
        duplicatedProcessDefinition.setId(processDefinitionId); //it has the same id as the first one, so it will be considered as duplicate
        duplicatedProcessDefinition.setKey("myProcessUpdated");
        duplicatedProcessDefinition.setName("My Process updated");
        duplicatedProcessDefinition.setDescription("Updated description");
        producer.send(new CloudProcessDeployedEventImpl(initialProcessDefinition),
                      new CloudProcessDeployedEventImpl(duplicatedProcessDefinition));

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = restTemplate.getProcDefinitions();

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

    @Test
    public void shouldUpdateProcessModelOnDuplicate() throws Exception {
        //given
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(UUID.randomUUID().toString());
        processDefinition.setKey("myFirstProcessKey");
        processDefinition.setName("My First Process");

        CloudProcessDeployedEventImpl firstProcessDeployedEvent = new CloudProcessDeployedEventImpl(processDefinition);
        firstProcessDeployedEvent.setProcessModelContent(StreamUtils.copyToString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("parse-for-test/processWithVariables.bpmn20.xml"),
                StandardCharsets.UTF_8));
        CloudProcessDeployedEventImpl secondProcessDeployedEvent = new CloudProcessDeployedEventImpl(processDefinition);
        secondProcessDeployedEvent.setProcessModelContent(StreamUtils.copyToString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("parse-for-test/SimpleProcess.bpmn20.xml"),
                StandardCharsets.UTF_8));
        producer.send(firstProcessDeployedEvent,
                      secondProcessDeployedEvent);

        //when
        ResponseEntity<String> responseEntity = restTemplate.getProcDefinitionModel(processDefinition.getId());

        //then
        assertThat(responseEntity.getBody())
                .isXmlEqualToContentOf(new File("src/test/resources/parse-for-test/SimpleProcess.bpmn20.xml"));
    }

}
