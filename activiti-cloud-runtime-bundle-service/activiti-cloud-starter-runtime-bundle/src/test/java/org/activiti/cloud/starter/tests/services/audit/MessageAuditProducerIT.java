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

import java.util.Collections;
import java.util.List;
import org.activiti.api.process.model.BPMNMessage;
import org.activiti.api.process.model.MessageSubscription;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.events.MessageSubscriptionEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.helper.MessageRestTemplate;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.apache.groovy.util.Maps;
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
import static org.activiti.api.process.model.builders.MessagePayloadBuilder.receive;
import static org.activiti.api.process.model.builders.MessagePayloadBuilder.start;
import static org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED;
import static org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents.MESSAGE_SENT;
import static org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents.MESSAGE_WAITING;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.ALL_REQUIRED_HEADERS;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.RUNTIME_BUNDLE_INFO_HEADERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = ServicesAuditITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class MessageAuditProducerIT {

    private static final String CATCH_MESSAGE = "catchMessage";

    @Autowired
    private MessageRestTemplate messageRestTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @BeforeEach
    public void setUp() {
        streamHandler.clear();
    }

    @Test
    public void shouldAuditBPMNEventsMessagesAreProduced() {
        //when
        ResponseEntity<CloudProcessInstance> startResponse =
            messageRestTemplate.message(start("auditStartMessage").withBusinessKey("businessId")
                .withVariable("correlationKey", "correlationId")
                .build());

        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(messageRestTemplate
            .message(receive("auditEventSubprocessMessage").withCorrelationKey("correlationId")
                .build())
            .getStatusCode())
            .isEqualTo(HttpStatus.OK);

        assertThat(messageRestTemplate
            .message(receive("auditBoundaryMessage").withCorrelationKey("correlationId")
                .withVariable("customerKey", "customerId")
                .build())
            .getStatusCode())
            .isEqualTo(HttpStatus.OK);

        assertThat(messageRestTemplate
            .message(receive("auditInteremdiateCatchMessage").withCorrelationKey("customerId")
                .withVariable("invoiceKey", "invoiceId")
                .build())
            .getStatusCode())
            .isEqualTo(HttpStatus.OK);

        // then
        CloudProcessInstance processInstance = startResponse.getBody();

        await("Audit BPMNMessage Events").untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders())
                .containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(receivedEvents)
                .filteredOn(CloudBPMNMessageEvent.class::isInstance)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionKey,
                    CloudRuntimeEvent::getProcessDefinitionVersion,
                    CloudRuntimeEvent::getBusinessKey,
                    event -> bpmnMessage(event).getElementId(),
                    event -> bpmnMessage(event).getProcessDefinitionId(),
                    event -> bpmnMessage(event).getProcessInstanceId(),
                    event -> bpmnMessage(event).getMessagePayload().getName(),
                    event -> bpmnMessage(event).getMessagePayload().getCorrelationKey(),
                    event -> bpmnMessage(event).getMessagePayload().getBusinessKey(),
                    event -> bpmnMessage(event).getMessagePayload().getVariables()
                )
                .containsExactly(
                    tuple(MESSAGE_RECEIVED,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "startMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditStartMessage",
                        null,
                        processInstance.getBusinessKey(),
                        Collections.singletonMap("correlationKey", "correlationId")
                    ),
                    tuple(MESSAGE_WAITING,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "startMessageEventSubprocessEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditEventSubprocessMessage",
                        "correlationId",
                        processInstance.getBusinessKey(),
                        null
                    ),
                    tuple(MESSAGE_SENT,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "intermediateThrowMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditIntermediateThrowMessage",
                        "correlationId",
                        processInstance.getBusinessKey(),
                        Collections.singletonMap("correlationKey", "correlationId")
                    ),
                    tuple(MESSAGE_WAITING,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "boundaryMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditBoundaryMessage",
                        "correlationId",
                        processInstance.getBusinessKey(),
                        null
                    ),
                    tuple(MESSAGE_RECEIVED,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "startMessageEventSubprocessEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditEventSubprocessMessage",
                        "correlationId",
                        processInstance.getBusinessKey(),
                        null
                    ),
                    tuple(MESSAGE_RECEIVED,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "boundaryMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditBoundaryMessage",
                        "correlationId",
                        processInstance.getBusinessKey(),
                        Collections.singletonMap("customerKey", "customerId")
                    ),
                    tuple(MESSAGE_WAITING,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "intermediateCatchMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditInteremdiateCatchMessage",
                        "customerId",
                        processInstance.getBusinessKey(),
                        null
                    ),
                    tuple(MESSAGE_RECEIVED,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "intermediateCatchMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditInteremdiateCatchMessage",
                        "customerId",
                        processInstance.getBusinessKey(),
                        Collections.singletonMap("invoiceKey", "invoiceId")
                    ),
                    tuple(MESSAGE_SENT,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        "throwEndMessageEvent",
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "auditThrowEndMessage",
                        "invoiceId",
                        processInstance.getBusinessKey(),
                        Maps.of("correlationKey", "correlationId",
                            "customerKey", "customerId",
                            "invoiceKey", "invoiceId")
                    )
                );

        });

    }

    @Test
    public void should_produceCloudMessageSubscriptionCancelledEvent_when_processIsDeleted() {
        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate
            .startProcess(
                new StartProcessPayloadBuilder()
                    .withProcessDefinitionKey(CATCH_MESSAGE)
                    .withVariable("correlationKey", "foo")
                    .withName("processInstanceName")
                    .withBusinessKey("businessKey")
                    .build());

        assertThat(startProcessEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        CloudProcessInstance processInstance = startProcessEntity.getBody();

        await("Audit BPMNMessage Events").untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders())
                .containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(receivedEvents)
                .filteredOn(CloudBPMNMessageEvent.class::isInstance)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionKey,
                    CloudRuntimeEvent::getProcessDefinitionVersion,
                    CloudRuntimeEvent::getBusinessKey,
                    event -> bpmnMessage(event).getProcessDefinitionId(),
                    event -> bpmnMessage(event).getProcessInstanceId(),
                    event -> bpmnMessage(event).getMessagePayload().getName(),
                    event -> bpmnMessage(event).getMessagePayload().getCorrelationKey(),
                    event -> bpmnMessage(event).getMessagePayload().getBusinessKey()
                )
                .contains(
                    tuple(MESSAGE_WAITING,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "testMessage",
                        "foo",
                        processInstance.getBusinessKey())
                );

        });

        ResponseEntity<CloudProcessInstance> deleteProcessEntity = processInstanceRestTemplate
            .delete(startProcessEntity);
        assertThat(deleteProcessEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders())
                .containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(receivedEvents)
                .filteredOn(CloudMessageSubscriptionCancelledEvent.class::isInstance)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessDefinitionId,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getProcessDefinitionKey,
                    CloudRuntimeEvent::getProcessDefinitionVersion,
                    CloudRuntimeEvent::getBusinessKey,
                    event -> messageSubscription(event).getProcessDefinitionId(),
                    event -> messageSubscription(event).getProcessInstanceId(),
                    event -> messageSubscription(event).getEventName(),
                    event -> messageSubscription(event).getConfiguration(),
                    event -> messageSubscription(event).getBusinessKey()
                )
                .contains(
                    tuple(
                        MessageSubscriptionEvent.MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED,
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        processInstance.getProcessDefinitionKey(),
                        1, // version
                        processInstance.getBusinessKey(),
                        processInstance.getProcessDefinitionId(),
                        processInstance.getId(),
                        "testMessage",
                        "foo",
                        processInstance.getBusinessKey())
                );

        });

    }


    private BPMNMessage bpmnMessage(CloudRuntimeEvent<?, ?> event) {
        return CloudBPMNMessageEvent.class.cast(event).getEntity();
    }

    private MessageSubscription messageSubscription(CloudRuntimeEvent<?, ?> event) {
        return CloudMessageSubscriptionCancelledEvent.class.cast(event).getEntity();
    }
}
