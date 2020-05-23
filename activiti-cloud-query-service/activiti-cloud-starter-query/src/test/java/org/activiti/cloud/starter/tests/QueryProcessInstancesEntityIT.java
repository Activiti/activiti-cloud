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

import java.util.Collection;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
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
public class QueryProcessInstancesEntityIT {

    private static final String PROC_URL = "/v1/process-instances";
    private static final String ADMIN_PROC_URL = "/admin/v1/process-instances";

    private static final ParameterizedTypeReference<PagedModel<ProcessInstanceEntity>> PAGED_PROCESS_INSTANCE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<ProcessInstanceEntity>>() {
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

    @BeforeEach
    public void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        processInstanceBuilder = new ProcessInstanceEventContainedBuilder(eventsAggregator);
    }

    @AfterEach
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
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getName,
                                ProcessInstanceEntity::getStatus)
                    .contains(tuple(completedProcess.getId(),
                                    "first",
                                    ProcessInstance.ProcessInstanceStatus.COMPLETED),
                              tuple(runningProcess.getId(),
                                    "second",
                                    ProcessInstance.ProcessInstanceStatus.RUNNING));
        });

        await().untilAsserted(() -> {

            //and filter by status
            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntityFiltered = testRestTemplate.exchange(PROC_URL + "?status={status}",
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
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstances();

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

    @Test
    public void shouldGetProcessDefinitionVersion() {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId("process-instance-id");
        process.setName("process");
        process.setProcessDefinitionKey("process-definition-key");
        process.setProcessDefinitionId("process-definition-id");
        process.setProcessDefinitionVersion(10);

        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(process),
                                   new CloudProcessStartedEventImpl(process,
                                                       null,
                                                       null));


        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);


        });
    }

    @Test
    public void shouldSuspendResumeProcess() {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId("process-instance-id");
        process.setName("process");
        process.setProcessDefinitionKey("process-definition-key");
        process.setProcessDefinitionId("process-definition-id");
        process.setProcessDefinitionVersion(10);

        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(process),
                                   new CloudProcessStartedEventImpl(process,
                                                       null,
                                                       null));


        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);

        });

        eventsAggregator.addEvents(new CloudProcessSuspendedEventImpl(process));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);
            assertThat(responseEntity.getBody().getProcessDefinitionKey()).isEqualTo("process-definition-key");
            assertThat(responseEntity.getBody().getStatus()).isEqualTo(ProcessInstanceStatus.SUSPENDED);

        });

        eventsAggregator.addEvents(new CloudProcessResumedEventImpl(process));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId(),
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<ProcessInstance>() {
                                                                                       });

            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getProcessDefinitionVersion()).isEqualTo(10);
            assertThat(responseEntity.getBody().getProcessDefinitionKey()).isEqualTo("process-definition-key");
            assertThat(responseEntity.getBody().getStatus()).isEqualTo(ProcessInstanceStatus.RUNNING);

        });

    }

    @Test
    public void shouldGetProcessInstancesFilteredByNameDescription() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("Process for filter");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("Process");

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {

            //when
            ResponseEntity<PagedModel<ProcessInstanceEntity>> responseEntity = executeRequestGetProcInstancesFiltered("for filter",null);

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstanceEntity> processInstanceEntities = responseEntity.getBody().getContent();
            assertThat(processInstanceEntities)
                    .extracting(ProcessInstanceEntity::getId,
                                ProcessInstanceEntity::getName,
                                ProcessInstanceEntity::getStatus)
                    .contains(tuple(completedProcess.getId(),
                                    completedProcess.getName(),
                                    ProcessInstance.ProcessInstanceStatus.COMPLETED));
        });
    }

    @Test
    public void shouldGetProcessInstancesAsAdmin() {
        //given
        ProcessInstance completedProcess = processInstanceBuilder.aCompletedProcessInstance("Process for filter");
        ProcessInstance runningProcess = processInstanceBuilder.aRunningProcessInstance("Process");

        eventsAggregator.sendAll();

        keycloakTokenProducer.setKeycloakTestUser("hradmin");
        await().untilAsserted(() -> {

            ResponseEntity<PagedModel<CloudProcessInstance>> responseEntity = testRestTemplate.exchange(ADMIN_PROC_URL + "?page=0&size=10",
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       new ParameterizedTypeReference<PagedModel<CloudProcessInstance>>() {
                                                                                       });
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .extracting(
                    CloudProcessInstance::getId,
                    CloudProcessInstance::getName,
                    CloudProcessInstance::getStatus)
                    .containsExactly(
                            tuple(completedProcess.getId(),
                                  completedProcess.getName(),
                                  ProcessInstanceStatus.COMPLETED),
                            tuple(runningProcess.getId(),
                                  runningProcess.getName(),
                                  ProcessInstanceStatus.RUNNING)
                    );
        });
    }

    private ResponseEntity<PagedModel<ProcessInstanceEntity>> executeRequestGetProcInstances() {

        return testRestTemplate.exchange(PROC_URL,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }

    private ResponseEntity<PagedModel<ProcessInstanceEntity>> executeRequestGetProcInstancesFiltered(String name,String description) {
        String url=PROC_URL;
        boolean add = false;
        if (name != null || description != null) {
            url += "?";
            if (name != null) {
                url += "name=" + name;
                add = true;
            }
            if (description != null) {
                if (add) {
                    url += "&";
                }
                url += "description=" + description;
            }

        }
        return testRestTemplate.exchange(url,
                                         HttpMethod.GET,
                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                         PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }
}
