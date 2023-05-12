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

import java.util.UUID;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.runtime.model.impl.ProcessCandidateStarterGroupImpl;
import org.activiti.api.runtime.model.impl.ProcessCandidateStarterUserImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.test.ProcessDefinitionRestTemplate;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@Import({ ProcessDefinitionRestTemplate.class, TestChannelBinderConfiguration.class })
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
public class QueryProcessDefinitionCandidateStartersIT {

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

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
        identityTokenProducer.withTestUser("hruser");
    }

    @AfterEach
    public void tearDown() {
        processModelRepository.deleteAll();
        processDefinitionRepository.deleteAll();
        identityTokenProducer.withDefaultValues();
    }

    @Test
    public void shouldNotGetProcessDefinitionsWhereCurrentUserIsNotACandidate() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = restTemplate.getProcDefinitions();

        //then
        assertThat(responseEntity.getBody()).isNotNull().isEmpty();
    }

    @Test
    public void shouldGetProcessDefinitionsWhereCurrentUserIsACandidateUser() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());

        CloudProcessCandidateStarterUserAddedEventImpl candidateStarterUserAddedEvent = new CloudProcessCandidateStarterUserAddedEventImpl(
            new ProcessCandidateStarterUserImpl(firstProcessDefinition.getId(), "hruser")
        );

        producer.send(
            new CloudProcessDeployedEventImpl(firstProcessDefinition),
            new CloudProcessDeployedEventImpl(secondProcessDefinition),
            candidateStarterUserAddedEvent
        );

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = restTemplate.getProcDefinitions();

        //then
        assertThat(responseEntity.getBody())
            .isNotNull()
            .extracting(ProcessDefinition::getId)
            .containsExactly(firstProcessDefinition.getId());
    }

    @Test
    public void shouldGetProcessDefinitionsWhereCurrentUserIsAMemberOfCandidateGroup() {
        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(UUID.randomUUID().toString());

        ProcessDefinitionImpl secondProcessDefinition = new ProcessDefinitionImpl();
        secondProcessDefinition.setId(UUID.randomUUID().toString());

        CloudProcessCandidateStarterGroupAddedEventImpl candidateStarterGroupAddedEvent = new CloudProcessCandidateStarterGroupAddedEventImpl(
            new ProcessCandidateStarterGroupImpl(firstProcessDefinition.getId(), "hr")
        );

        producer.send(
            new CloudProcessDeployedEventImpl(firstProcessDefinition),
            new CloudProcessDeployedEventImpl(secondProcessDefinition),
            candidateStarterGroupAddedEvent
        );

        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> responseEntity = restTemplate.getProcDefinitions();

        //then
        assertThat(responseEntity.getBody())
            .isNotNull()
            .extracting(ProcessDefinition::getId)
            .containsExactly(firstProcessDefinition.getId());
    }
}
