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
import static org.activiti.api.process.model.events.BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.ALL_REQUIRED_HEADERS;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.RUNTIME_BUNDLE_INFO_HEADERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.BPMNError;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
public class ErrorAuditProducerIT {

    private static final String ERROR_START_EVENT_SUBPROCESS = "errorStartEventSubProcess";

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @BeforeEach
    public void setUp() {
        streamHandler.clear();
    }

    @Test
    public void should_produceBpmnErrorEvents_when_processIsExecuted() {

        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(new StartProcessPayloadBuilder()
            .withProcessDefinitionKey(ERROR_START_EVENT_SUBPROCESS)
            .withName("processInstanceName")
            .withBusinessKey("businessKey")
            .build());

        assertThat(startProcessEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        CloudProcessInstance processInstance = startProcessEntity.getBody();

        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(receivedEvents)
                .filteredOn(event -> (event.getEventType().equals(ACTIVITY_STARTED) ||
                    event.getEventType().equals(ACTIVITY_COMPLETED)))
                .extracting(CloudRuntimeEvent::getEventType,
                    event -> ((BPMNActivity) event.getEntity()).getActivityType(),
                    event -> ((BPMNActivity) event.getEntity()).getElementId(),
                    event -> ((BPMNActivity) event.getEntity()).getProcessInstanceId())
                .contains(tuple(ACTIVITY_STARTED,
                    "endEvent",
                    "subEnd",
                    processInstance.getId()),
                    tuple(ACTIVITY_STARTED,
                        "startEvent",
                        "subStart1",
                        processInstance.getId()),
                    tuple(ACTIVITY_COMPLETED,
                        "startEvent",
                        "subStart1",
                        processInstance.getId()));

            assertThat(receivedEvents)
                .filteredOn(CloudBPMNErrorReceivedEvent.class::isInstance)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionKey,
                    CloudRuntimeEvent::getProcessDefinitionVersion,
                    CloudRuntimeEvent::getBusinessKey,
                    event -> bpmnError(event).getElementId(),
                    event -> bpmnError(event).getProcessDefinitionId(),
                    event -> bpmnError(event).getProcessInstanceId(),
                    event -> bpmnError(event).getErrorCode(),
                    event -> bpmnError(event).getErrorId(),
                    event -> bpmnError(event).getActivityType(),
                    event -> bpmnError(event).getActivityName()
                )
                .containsExactly(
                    tuple(ERROR_RECEIVED,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        processInstance.getProcessDefinitionVersion(),
                        processInstance.getBusinessKey(),
                        "subStart1",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "123",
                        "errorId",
                        null,
                        null
                    )
                );
        });

    }

    private BPMNError bpmnError(CloudRuntimeEvent<?, ?> event) {
        return CloudBPMNErrorReceivedEvent.class.cast(event).getEntity();
    }
}
