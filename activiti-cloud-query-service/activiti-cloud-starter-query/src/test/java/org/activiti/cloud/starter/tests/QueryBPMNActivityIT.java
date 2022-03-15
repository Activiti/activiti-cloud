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
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class QueryBPMNActivityIT {

    private static final String PROC_URL = "/v1/process-instances";

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

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

    private String processDefinitionId2 = UUID.randomUUID().toString();

    private EventsAggregator eventsAggregator;

    @BeforeEach
    public void setUp() throws IOException {
        identityTokenProducer.setTestUser("hruser");

        eventsAggregator = new EventsAggregator(producer);

        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processDefinitionId2);
        processDefinition.setKey("mySimpleProcess2");
        processDefinition.setName("My Simple Process");

        CloudProcessDeployedEventImpl processDeployedEvent = new CloudProcessDeployedEventImpl(processDefinition);
        processDeployedEvent.setProcessModelContent(new String(Files.readAllBytes(Paths.get("src/test/resources/parse-for-test/SimpleProcessWithoutDiagram.bpmn20.xml")),
                                                                     StandardCharsets.UTF_8));

        producer.send(processDeployedEvent);
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
    public void shouldHandleCyclicalBPMNActivityEvents() throws InterruptedException {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId(UUID.randomUUID().toString());
        process.setName("process");
        process.setProcessDefinitionKey("mySimpleProcess2");
        process.setProcessDefinitionId(processDefinitionId2);
        process.setProcessDefinitionVersion(1);

        BPMNActivityImpl startActivity = new BPMNActivityImpl("startEvent1", "", "startEvent");
        startActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        startActivity.setProcessInstanceId(process.getId());
        startActivity.setExecutionId("executionId");

        BPMNSequenceFlowImpl sequenceFlow = new BPMNSequenceFlowImpl("sid-68945AF1-396F-4B8A-B836-FC318F62313F", "startEvent1", "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94");
        sequenceFlow.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow.setProcessInstanceId(process.getId());

        BPMNActivityImpl taskActivity = new BPMNActivityImpl("sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94", "Perform Action", "userTask");
        taskActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        taskActivity.setProcessInstanceId(process.getId());
        taskActivity.setExecutionId("executionId");

        eventsAggregator.addEvents(new CloudProcessCreatedEventImpl(process),
                                   new CloudProcessStartedEventImpl(process, null, null),
                                   new CloudBPMNActivityStartedEventImpl(startActivity, processDefinitionId, process.getId()),
                                   new CloudBPMNActivityCompletedEventImpl(startActivity, processDefinitionId, process.getId()),
                                   new CloudSequenceFlowTakenEventImpl(sequenceFlow),
                                   new CloudBPMNActivityStartedEventImpl(taskActivity, processDefinitionId, process.getId()),
                                   new CloudBPMNActivityCompletedEventImpl(taskActivity, processDefinitionId, process.getId())
        );

        //when
        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
            List<BPMNActivityEntity> activities = bpmnActivityRepository.findByProcessInstanceId(process.getId());

            assertThat(activities).hasSize(2);
            assertThat(activities).extracting(BPMNActivityEntity::getElementId, BPMNActivityEntity::getActivityType,  BPMNActivityEntity::getStatus)
                                  .containsExactly(tuple(startActivity.getElementId(),startActivity.getActivityType(), BPMNActivityEntity.BPMNActivityStatus.COMPLETED),
                                                   tuple(taskActivity.getElementId(),taskActivity.getActivityType(), BPMNActivityEntity.BPMNActivityStatus.COMPLETED));
        });

        BPMNActivityImpl restartTaskActivity = new BPMNActivityImpl("sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94", "Perform Action", "userTask");
        restartTaskActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        restartTaskActivity.setProcessInstanceId(process.getId());
        restartTaskActivity.setExecutionId("executionId");

        eventsAggregator.addEvents(new CloudBPMNActivityStartedEventImpl(taskActivity, processDefinitionId, process.getId()));

        eventsAggregator.sendAll();

        await().untilAsserted(() -> {
            List<BPMNActivityEntity> activities = bpmnActivityRepository.findByProcessInstanceId(process.getId());

            assertThat(activities).hasSize(2);
            assertThat(activities).extracting(BPMNActivityEntity::getElementId, BPMNActivityEntity::getActivityType,  BPMNActivityEntity::getStatus)
                                  .containsExactly(tuple(startActivity.getElementId(),startActivity.getActivityType(), BPMNActivityEntity.BPMNActivityStatus.COMPLETED),
                                                   tuple(taskActivity.getElementId(),taskActivity.getActivityType(), BPMNActivityEntity.BPMNActivityStatus.STARTED));
        });
    }
}
