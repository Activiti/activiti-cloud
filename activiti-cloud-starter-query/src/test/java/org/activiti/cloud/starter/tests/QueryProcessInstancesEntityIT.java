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
import static org.awaitility.Awaitility.await;

import java.util.Collection;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
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
public class QueryProcessInstancesEntityIT {

    private static final String PROC_URL = "/v1/process-instances";
    private static final String ADMIN_PROC_URL = "/admin/v1/process-instances";
    
    private static final ParameterizedTypeReference<PagedResources<ProcessInstanceEntity>> PAGED_PROCESS_INSTANCE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<ProcessInstanceEntity>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    private EventsAggregator eventsAggregator;

    private ProcessInstanceEventContainedBuilder processInstanceBuilder;

    @Before
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
    }

    @After
    public void tearDown() {
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableProcInstancesAndFilteredProcessInstances() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("first");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("second");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getStatus)
                    .contains(tuple(completedProcess.getId(),
                                    ProcessInstance.ProcessInstanceStatus.COMPLETED),
                              tuple(runningProcess.getId(),
                                    ProcessInstance.ProcessInstanceStatus.RUNNING));
        });

        await().untilAsserted(() -> {

            //and filter by status
            //when
            ResponseEntity<PagedResources<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(PROC_URL + "?status={status}",
                                                                                                                     HttpMethod.GET,
                                                                                                                     keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                                                     PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
                                                                                                                     ProcessInstance.ProcessInstanceStatus.COMPLETED);

            //then
            assertThat(responseEntityFiltered).isNotNull();
            assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> filteredProcessInstanceEntities = responseEntityFiltered.getBody().getContent();
            assertThat(filteredProcessInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getStatus)
                    .containsExactly(tuple(completedProcess.getId(),
                                           ProcessInstance.ProcessInstanceStatus.COMPLETED));
        });
    }
    
    @Test
    public void shouldGetProcessWithUpdatedInfo() {
        //given
        ProcessInstance process = processInstanceBuilder.aRunningProcessInstance("running");

        
        eventsAggregator.sendAll();
        
        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getStatus)
                    .contains(tuple(process.getId(),
                                    ProcessInstance.ProcessInstanceStatus.RUNNING));
        });
        
        //when
        ProcessInstanceImpl updatedProcess = new ProcessInstanceImpl();
        updatedProcess.setId(process.getId());
        updatedProcess.setBusinessKey("businessKey");
        updatedProcess.setName("name");
        
        
        producer.send(new CloudProcessUpdatedEventImpl(updatedProcess));

        await().untilAsserted(() -> {
     
             ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                            HttpMethod.GET,
                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                            new ParameterizedTypeReference<ProcessInstance>() {
                                                                            });
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isNotNull();
            
            ProcessInstance responseProcess = responseEntity.getBody();
            assertThat(responseProcess.getBusinessKey()).isEqualTo(updatedProcess.getBusinessKey());
            assertThat(responseProcess.getName()).isEqualTo(updatedProcess.getName());

        });
    }

    
    @Test
    public void shouldGetAdminProcessInfo() {
        //given
        ProcessInstance process = processInstanceBuilder.aRunningProcessInstance("running");

        
        eventsAggregator.sendAll();
        
  
        await().untilAsserted(() -> {
             keycloakTokenProducer.setKeycloakTestUser("hradmin");
     
             ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(ADMIN_PROC_URL + "/" + process.getId(),
                                                                            HttpMethod.GET,
                                                                            keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                            new ParameterizedTypeReference<ProcessInstance>() {
                                                                            });
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getId()).isNotNull();
            
            ProcessInstance responseProcess = responseEntity.getBody();
            assertThat(responseProcess.getId()).isEqualTo(process.getId());

        });
    }
    
    private ResponseEntity<PagedResources<ProcessInstanceEntity>> executeRequestGetProcInstances() {

        return testRestTemplate.exchange(PROC_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }
}
