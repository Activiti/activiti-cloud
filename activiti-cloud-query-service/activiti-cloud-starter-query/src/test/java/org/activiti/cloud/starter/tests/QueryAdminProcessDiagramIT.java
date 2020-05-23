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
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test-admin.properties")
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryAdminProcessDiagramIT {

    private static final String PROC_URL = "/admin/v1/process-instances";

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ProcessModelRepository processModelRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private BPMNActivityRepository bpmnActivityRepository;

    @Autowired
    private BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    @Autowired
    private MyProducer producer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private String processDefinitionId = UUID.randomUUID().toString();


    private EventsAggregator eventsAggregator;

    @BeforeEach
    public void setUp() throws IOException {
        keycloakTokenProducer.setKeycloakTestUser("hradmin");

        eventsAggregator = new EventsAggregator(producer);

        //given
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(processDefinitionId);
        firstProcessDefinition.setKey("mySimpleProcess");
        firstProcessDefinition.setName("My Simple Process");

        CloudProcessDeployedEventImpl firstProcessDeployedEvent = new CloudProcessDeployedEventImpl(firstProcessDefinition);
        firstProcessDeployedEvent.setProcessModelContent(new String(Files.readAllBytes(Paths.get("src/test/resources/parse-for-test/SimpleProcess.bpmn20.xml")),
                                                                    StandardCharsets.UTF_8));

        producer.send(firstProcessDeployedEvent);
    }

    @AfterEach
    public void tearDown() {
        processModelRepository.deleteAll();
        processDefinitionRepository.deleteAll();
        processInstanceRepository.deleteAll();
        bpmnActivityRepository.deleteAll();
        bpmnSequenceFlowRepository.deleteAll();
    }

    @Test
    public void shouldGetProcessInstanceDiagramAdmin() throws InterruptedException {
        //given
        ProcessInstanceImpl process = startSimpleProcessInstance();

        //when
        eventsAggregator.sendAll();

        //then
        await().untilAsserted(() -> {
            assertThat(bpmnActivityRepository.findByProcessInstanceId(process.getId())).hasSize(2);
            assertThat(bpmnSequenceFlowRepository.findByProcessInstanceId(process.getId())).hasSize(1);
        });

        await().atMost(Durations.ONE_MINUTE).untilAsserted(() -> {
            //when
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId() + "/diagram",
                                                                                       HttpMethod.GET,
                                                                                       keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                       String.class);
            //then
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody()).contains("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN'");
        });
    }

    @Test
    public void shouldNotGetProcessInstanceDiagramAdmin() throws InterruptedException {
        //given
        keycloakTokenProducer.setKeycloakTestUser("hruser");

        ProcessInstanceImpl process = startSimpleProcessInstance();

        //when
        eventsAggregator.sendAll();

        //then
        await().untilAsserted(() -> {
            assertThat(bpmnActivityRepository.findByProcessInstanceId(process.getId())).hasSize(2);
            assertThat(bpmnSequenceFlowRepository.findByProcessInstanceId(process.getId())).hasSize(1);
        });

        await().atMost(Durations.ONE_MINUTE).untilAsserted(() -> {
           //when
           ResponseEntity<Map<String,Object>> responseEntity = testRestTemplate.exchange(PROC_URL + "/" + process.getId() + "/diagram",
                                                                                         HttpMethod.GET,
                                                                                         keycloakTokenProducer.entityWithAuthorizationHeader(),
                                                                                         new ParameterizedTypeReference<Map<String, Object>>() {
                                                                                       });
           //then
           assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        });
    }

    protected ProcessInstanceImpl startSimpleProcessInstance() {
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId(UUID.randomUUID().toString());
        process.setName("process");
        process.setProcessDefinitionKey("mySimpleProcess");
        process.setProcessDefinitionId(processDefinitionId);
        process.setProcessDefinitionVersion(1);

        BPMNActivityImpl startActivity = new BPMNActivityImpl("startEvent1", "", "startEvent");
        startActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        startActivity.setProcessInstanceId(process.getId());
        startActivity.setExecutionId(UUID.randomUUID().toString());

        BPMNSequenceFlowImpl sequenceFlow = new BPMNSequenceFlowImpl("sid-68945AF1-396F-4B8A-B836-FC318F62313F", "startEvent1", "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94");
        sequenceFlow.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow.setProcessInstanceId(process.getId());

        BPMNActivityImpl taskActivity = new BPMNActivityImpl("sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94", "Perform Action", "userTask");
        taskActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        taskActivity.setProcessInstanceId(process.getId());
        taskActivity.setExecutionId(UUID.randomUUID().toString());

        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(process),
                                   new CloudProcessStartedEventImpl(process, null, null),
                                   new CloudBPMNActivityStartedEventImpl(startActivity, processDefinitionId, process.getId()),
                                   new CloudBPMNActivityCompletedEventImpl(startActivity, processDefinitionId, process.getId()),
                                   new CloudSequenceFlowTakenEventImpl(sequenceFlow),
                                   new CloudBPMNActivityStartedEventImpl(taskActivity, processDefinitionId, process.getId())
        );

        return process;

    }
}
