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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessCompletedEvent;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessCreatedEvent;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessStartedEvent;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class QueryProcessInstancesIT {

    private static final String PROC_URL = "/v1/process-instances";
    private static final ParameterizedTypeReference<PagedResources<ProcessInstance>> PAGED_PROCESS_INSTANCE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
    };

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private MyProducer producer;

    @After
    public void tearDown() throws Exception {
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetAvailableProcInstancesAndFilteredProcessInstaces() throws Exception {
        //given

        // a completed process
        List<ProcessEngineEvent> createStartCompleteProcess = new ArrayList<ProcessEngineEvent>();
        createStartCompleteProcess.addAll(Arrays.asList(aProcessCreatedEvent(System.currentTimeMillis(),
                "10",
                "defId",
                "15")));
        createStartCompleteProcess.addAll(Arrays.asList(aProcessStartedEvent(System.currentTimeMillis(),
                "10",
                "defId",
                "15")));
        createStartCompleteProcess.addAll(Arrays.asList(aProcessCompletedEvent(System.currentTimeMillis(),
                "10",
                "defId",
                "15")));

        // a running process
        List<ProcessEngineEvent> startRunningProcess = new ArrayList<ProcessEngineEvent>();
        createStartCompleteProcess.addAll(Arrays.asList(aProcessCreatedEvent(System.currentTimeMillis(),
                "11",
                "defId",
                "16")));
        createStartCompleteProcess.addAll(Arrays.asList(aProcessStartedEvent(System.currentTimeMillis(),
                "11",
                "defId",
                "16")));

        List<ProcessEngineEvent> eventsForTest = new ArrayList<>();
        eventsForTest.addAll(createStartCompleteProcess);
        eventsForTest.addAll(startRunningProcess);

        producer.send(eventsForTest.toArray(new ProcessEngineEvent[]{}));


        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {

            //when
            ResponseEntity<PagedResources<ProcessInstance>> responseEntity = executeRequestGetProcInstances();

            //then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstance> processInstances = responseEntity.getBody().getContent();
            assertThat(processInstances)
                    .extracting(ProcessInstance::getId,
                            ProcessInstance::getStatus)
                    .contains(tuple("15",
                            "COMPLETED"),
                            tuple("16",
                                    "RUNNING"));
        });

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {

            //and filter by status
            //when
            ResponseEntity<PagedResources<ProcessInstance>> responseEntityFiltered = testRestTemplate.exchange(PROC_URL + "?status={status}",
                    HttpMethod.GET,
                    getHeaderEntity(),
                    PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
                    "COMPLETED");

            //then
            assertThat(responseEntityFiltered).isNotNull();
            assertThat(responseEntityFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);

            Collection<ProcessInstance> filteredProcessInstances = responseEntityFiltered.getBody().getContent();
            assertThat(filteredProcessInstances)
                    .extracting(ProcessInstance::getId,
                            ProcessInstance::getStatus)
                    .containsExactly(tuple("15",
                            "COMPLETED"));
        });


    }


    private ResponseEntity<PagedResources<ProcessInstance>> executeRequestGetProcInstances() {

        return testRestTemplate.exchange(PROC_URL,
                HttpMethod.GET,
                getHeaderEntity(),
                PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }

    private HttpEntity getHeaderEntity(){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", keycloakTokenProducer.getTokenString());
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        return entity;
    }

}
