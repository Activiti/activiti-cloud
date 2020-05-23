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
package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
import static org.activiti.api.process.model.events.SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.ALL_REQUIRED_HEADERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = ServicesAuditITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ParallelGatewayAuditProducerIT {

    private static final String PARALLEL_GATEWAY_PROCESS = "basicParallelGateway";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;


    @Test
    public void testProcessExecutionWithParallelGateway() {
        //when
        streamHandler.getAllReceivedEvents().clear();
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(
            new StartProcessPayloadBuilder()
                .withProcessDefinitionKey(PARALLEL_GATEWAY_PROCESS)
                .build());
        String processInstanceId = processInstance.getBody().getId();

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(PROCESS_CREATED,
                    processInstanceId,
                    processInstanceId),
                    tuple(PROCESS_STARTED,
                        processInstanceId,
                        processInstanceId),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theStart"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theStart"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow1"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "task1"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "task1"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow2"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "parallelGateway"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "parallelGateway"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow3"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow5"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "task2"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "task3"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "task2"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "task3"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow4"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow6"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theEnd1"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theEnd2"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theEnd1"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theEnd2"),
                    tuple(PROCESS_COMPLETED,
                        processInstanceId,
                        processInstanceId)
                );

            assertThat(receivedEvents)
                .filteredOn(event -> (event.getEventType().equals(ACTIVITY_STARTED) ||
                    event.getEventType().equals(ACTIVITY_COMPLETED)) &&
                    ((BPMNActivity) event.getEntity()).getActivityType().equals("parallelGateway"))
                .extracting(CloudRuntimeEvent::getEventType,
                    event -> ((BPMNActivity) event.getEntity()).getActivityType(),
                    event -> ((BPMNActivity) event.getEntity()).getProcessInstanceId())
                .contains(tuple(ACTIVITY_STARTED,
                    "parallelGateway",
                    processInstanceId),
                    tuple(ACTIVITY_COMPLETED,
                        "parallelGateway",
                        processInstanceId)

                );


        });


    }


}
