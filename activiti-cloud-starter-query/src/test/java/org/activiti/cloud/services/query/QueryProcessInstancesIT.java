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

package org.activiti.cloud.services.query;

import java.util.Collection;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessCompletedEvent;
import static org.activiti.cloud.starters.test.MockProcessEngineEvent.aProcessStartedEvent;
import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QueryProcessInstancesIT {

    private static final String PROC_URL = "/v1/process-instances";
    private static final ParameterizedTypeReference<PagedResources<ProcessInstance>> PAGED_PROCESS_INSTANCE_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
    };

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
    public void shouldGetAvailableProcInstances() throws Exception {
        //given
        // a completed process
        producer.send(aProcessStartedEvent(System.currentTimeMillis(),
                                           "10",
                                           "defId",
                                           "15"));
        producer.send(aProcessCompletedEvent(System.currentTimeMillis(),
                                             "10",
                                             "defId",
                                             "15"));

        // a running process
        producer.send(aProcessStartedEvent(System.currentTimeMillis(),
                                           "11",
                                           "defId",
                                           "16"));

        waitForMessage();

        //when
        ResponseEntity<PagedResources<ProcessInstance>> responseEntity = executeRequestGetProcInstances();

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Collection<ProcessInstance> processInstances = responseEntity.getBody().getContent();
        assertThat(processInstances)
                .extracting(ProcessInstance::getProcessInstanceId,
                            ProcessInstance::getStatus)
                .contains(tuple("15",
                                "COMPLETED"),
                          tuple("16",
                                "RUNNING"));
    }

    @Test
    public void shouldFilterOnStatus() throws Exception {
        //given
        // a completed process
        producer.send(aProcessStartedEvent(System.currentTimeMillis(),
                                           "10",
                                           "defId",
                                           "15"));
        producer.send(aProcessCompletedEvent(System.currentTimeMillis(),
                                             "10",
                                             "defId",
                                             "15"));

        // a running process
        producer.send(aProcessStartedEvent(System.currentTimeMillis(),
                                           "11",
                                           "defId",
                                           "16"));

        waitForMessage();

        //when
        ResponseEntity<PagedResources<ProcessInstance>> responseEntity = testRestTemplate.exchange(PROC_URL + "?status={status}",
                                                                                                   HttpMethod.GET,
                                                                                                   null,
                                                                                                   PAGED_PROCESS_INSTANCE_RESPONSE_TYPE,
                                                                                                   "COMPLETED");

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Collection<ProcessInstance> processInstances = responseEntity.getBody().getContent();
        assertThat(processInstances)
                .extracting(ProcessInstance::getProcessInstanceId,
                            ProcessInstance::getStatus)
                .containsExactly(tuple("15",
                                       "COMPLETED"));
    }

    private ResponseEntity<PagedResources<ProcessInstance>> executeRequestGetProcInstances() {
        return testRestTemplate.exchange(PROC_URL,
                                         HttpMethod.GET,
                                         null,
                                         PAGED_PROCESS_INSTANCE_RESPONSE_TYPE);
    }

    private void waitForMessage() throws InterruptedException {
        Thread.sleep(500);
    }
}
