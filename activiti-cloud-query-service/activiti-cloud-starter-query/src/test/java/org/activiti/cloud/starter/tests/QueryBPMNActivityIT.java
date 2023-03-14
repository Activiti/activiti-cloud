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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
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
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@EnableAutoConfiguration(exclude = { WebMvcAutoConfiguration.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(initializers = {KeycloakContainerApplicationInitializer.class})
@Import(TestChannelBinderConfiguration.class)
public class QueryBPMNActivityIT {

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
    private ServiceTaskRepository serviceTaskRepository;

    @Autowired
    private MyProducer producer;

    private String processDefinitionId = UUID.randomUUID().toString();

    private EventsAggregator eventsAggregator;

    @Value("classpath:events/multi-instance-sequence.json")
    private Resource multiInstanceSequenceJson;

    @Value("classpath:events/multi-instance-sequence-legacy.json")
    private Resource multiInstanceSequenceJsonLegacy;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws IOException {
        identityTokenProducer.withTestUser("hruser");

        eventsAggregator = new EventsAggregator(producer);

        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processDefinitionId);
        processDefinition.setKey("mySimpleProcess2");
        processDefinition.setName("My Simple Process");

        CloudProcessDeployedEventImpl processDeployedEvent = new CloudProcessDeployedEventImpl(processDefinition);
        processDeployedEvent.setProcessModelContent(
            new String(
                Files.readAllBytes(
                    Paths.get("src/test/resources/parse-for-test/SimpleProcessWithoutDiagram.bpmn20.xml")
                ),
                StandardCharsets.UTF_8
            )
        );

        producer.send(processDeployedEvent);
    }

    @AfterEach
    public void tearDown() {
        processModelRepository.deleteAll();
        processDefinitionRepository.deleteAll();
        processInstanceRepository.deleteAll();
        bpmnActivityRepository.deleteAll();
        bpmnSequenceFlowRepository.deleteAll();
        serviceTaskRepository.deleteAll();
    }

    @Test
    public void shouldHandleCyclicalBPMNActivityEvents() throws InterruptedException {
        //given
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId(UUID.randomUUID().toString());
        process.setName("process");
        process.setProcessDefinitionKey("mySimpleProcess2");
        process.setProcessDefinitionId(processDefinitionId);
        process.setProcessDefinitionVersion(1);

        BPMNActivityImpl startActivity = new BPMNActivityImpl("startEvent1", "", "startEvent");
        startActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        startActivity.setProcessInstanceId(process.getId());
        startActivity.setExecutionId("executionId");

        BPMNSequenceFlowImpl sequenceFlow = new BPMNSequenceFlowImpl("sf-1", "startEvent1", "reviewTaskActivity");
        sequenceFlow.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow.setProcessInstanceId(process.getId());

        BPMNActivityImpl reviewTaskActivity = new BPMNActivityImpl("reviewTaskActivity", "Employee Review", "userTask");
        reviewTaskActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        reviewTaskActivity.setProcessInstanceId(process.getId());
        reviewTaskActivity.setExecutionId("executionId");

        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(process),
            new CloudProcessStartedEventImpl(process, null, null),
            new CloudBPMNActivityStartedEventImpl(startActivity, processDefinitionId, process.getId()),
            new CloudBPMNActivityCompletedEventImpl(startActivity, processDefinitionId, process.getId()),
            new CloudSequenceFlowTakenEventImpl(sequenceFlow),
            new CloudBPMNActivityStartedEventImpl(reviewTaskActivity, processDefinitionId, process.getId()),
            new CloudBPMNActivityCompletedEventImpl(reviewTaskActivity, processDefinitionId, process.getId())
        );

        //when
        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                List<BPMNActivityEntity> activities = bpmnActivityRepository.findByProcessInstanceId(process.getId());

                assertThat(activities).hasSize(2);
                assertThat(activities)
                    .extracting(
                        BPMNActivityEntity::getElementId,
                        BPMNActivityEntity::getActivityType,
                        BPMNActivityEntity::getStatus
                    )
                    .containsExactly(
                        tuple(
                            startActivity.getElementId(),
                            startActivity.getActivityType(),
                            BPMNActivityEntity.BPMNActivityStatus.COMPLETED
                        ),
                        tuple(
                            reviewTaskActivity.getElementId(),
                            reviewTaskActivity.getActivityType(),
                            BPMNActivityEntity.BPMNActivityStatus.COMPLETED
                        )
                    );
            });

        BPMNSequenceFlowImpl sequenceFlow2 = new BPMNSequenceFlowImpl(
            "sf-2",
            "reviewTaskActivity",
            "employeeTaskActivity"
        );
        sequenceFlow2.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow2.setProcessInstanceId(process.getId());

        BPMNActivityImpl employeeTaskActivity = new BPMNActivityImpl("employeeTaskActivity", "Employee", "userTask");
        employeeTaskActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        employeeTaskActivity.setProcessInstanceId(process.getId());
        employeeTaskActivity.setExecutionId("executionId");

        BPMNSequenceFlowImpl sequenceFlow3 = new BPMNSequenceFlowImpl(
            "sf-3",
            "employeeTaskActivity",
            "reviewTaskActivity"
        );
        sequenceFlow3.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow3.setProcessInstanceId(process.getId());

        eventsAggregator.addEvents(
            new CloudSequenceFlowTakenEventImpl(sequenceFlow2),
            new CloudBPMNActivityStartedEventImpl(employeeTaskActivity, processDefinitionId, process.getId()),
            new CloudBPMNActivityCompletedEventImpl(employeeTaskActivity, processDefinitionId, process.getId()),
            new CloudSequenceFlowTakenEventImpl(sequenceFlow3),
            new CloudBPMNActivityStartedEventImpl(reviewTaskActivity, processDefinitionId, process.getId())
        );

        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                List<BPMNActivityEntity> activities = bpmnActivityRepository.findByProcessInstanceId(process.getId());

                assertThat(activities).hasSize(3);
                assertThat(activities)
                    .extracting(
                        BPMNActivityEntity::getElementId,
                        BPMNActivityEntity::getActivityType,
                        BPMNActivityEntity::getStatus
                    )
                    .containsOnly(
                        tuple(
                            startActivity.getElementId(),
                            startActivity.getActivityType(),
                            BPMNActivityEntity.BPMNActivityStatus.COMPLETED
                        ),
                        tuple(
                            employeeTaskActivity.getElementId(),
                            employeeTaskActivity.getActivityType(),
                            BPMNActivityEntity.BPMNActivityStatus.COMPLETED
                        ),
                        tuple(
                            reviewTaskActivity.getElementId(),
                            reviewTaskActivity.getActivityType(),
                            BPMNActivityEntity.BPMNActivityStatus.STARTED
                        )
                    );
            });
    }

    @Test
    public void shouldReplayMultiInstanceSequenceBPMNActivityEvents() throws IOException {
        //given
        List<CloudRuntimeEvent> events = objectMapper.readValue(
            multiInstanceSequenceJson.getFile(),
            new TypeReference<List<CloudRuntimeEvent>>() {}
        );

        replayAuditEvents(events);
    }

    @Test
    public void shouldReplayMultiInstanceSequenceBPMNActivityEventsLegacy() throws IOException {
        //given
        List<CloudRuntimeEvent> events = objectMapper.readValue(
            multiInstanceSequenceJsonLegacy.getFile(),
            new TypeReference<List<CloudRuntimeEvent>>() {}
        );

        replayAuditEvents(events);
    }

    private void replayAuditEvents(List<CloudRuntimeEvent> events) {
        eventsAggregator.addEvents(events.toArray(new CloudRuntimeEvent[] {}));

        //when
        eventsAggregator.sendAll();

        //then
        String processInstanceId = events.get(0).getProcessInstanceId();

        await()
            .untilAsserted(() -> {
                Optional<ProcessInstanceEntity> result = processInstanceRepository.findById(processInstanceId);
                assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(ProcessInstanceEntity::getStatus)
                    .isEqualTo(ProcessInstance.ProcessInstanceStatus.COMPLETED);

                List<ServiceTaskEntity> serviceTasks = serviceTaskRepository.findByProcessInstanceId(processInstanceId);

                assertThat(serviceTasks)
                    .hasSize(1)
                    .extracting(ServiceTaskEntity::getActivityName, ServiceTaskEntity::getStatus)
                    .containsOnly(tuple("decisiontask-sequence", CloudBPMNActivity.BPMNActivityStatus.COMPLETED));

                List<BPMNActivityEntity> activities = bpmnActivityRepository.findByProcessInstanceId(processInstanceId);

                assertThat(activities)
                    .hasSize(4)
                    .extracting(BPMNActivityEntity::getActivityName, BPMNActivityEntity::getStatus)
                    .containsOnly(
                        tuple(null, CloudBPMNActivity.BPMNActivityStatus.COMPLETED),
                        tuple("usertaskdmnoutputform", CloudBPMNActivity.BPMNActivityStatus.COMPLETED),
                        tuple(null, CloudBPMNActivity.BPMNActivityStatus.COMPLETED),
                        tuple("decisiontask-sequence", CloudBPMNActivity.BPMNActivityStatus.COMPLETED)
                    );
            });
    }
}
