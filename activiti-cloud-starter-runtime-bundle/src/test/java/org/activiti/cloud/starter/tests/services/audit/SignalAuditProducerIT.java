/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.ALL_REQUIRED_HEADERS;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.RUNTIME_BUNDLE_INFO_HEADERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.starter.tests.helper.ProcessDefinitionRestTemplate;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.SignalRestTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SignalAuditProducerIT {

    private static final String SIGNAL_PROCESS = "broadcastSignalCatchEventProcess";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private SignalRestTemplate signalRestTemplate;

    @Autowired
    private ProcessDefinitionRestTemplate processDefinitionRestTemplate;

    @Test
    public void shouldProduceEventsWhenIntermediateSignalIsReceived() {

        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity1 = processInstanceRestTemplate.startProcess(new StartProcessPayloadBuilder()
                                                                                                                    .withProcessDefinitionKey(SIGNAL_PROCESS)
                                                                                                                    .withName("processInstanceName1")
                                                                                                                    .withBusinessKey("businessKey1")
                                                                                                                    .withVariables(Collections.emptyMap())
                                                                                                                    .build());

        ResponseEntity<CloudProcessInstance> startProcessEntity2 = processInstanceRestTemplate.startProcess(new StartProcessPayloadBuilder()
                                                                                                                    .withProcessDefinitionKey(SIGNAL_PROCESS)
                                                                                                                    .withName("processInstanceName2")
                                                                                                                    .withBusinessKey("businessKey2")
                                                                                                                    .withVariables(Collections.emptyMap())
                                                                                                                    .build());

        CloudProcessDefinition processWithSignalStart = processDefinitionRestTemplate.getProcessDefinitions().getBody().getContent()
                .stream()
                .filter(cloudProcessDefinition -> cloudProcessDefinition.getKey().equals("processWithSignalStart1"))
                .findAny()
                .orElse(null);
        assertThat(processWithSignalStart).isNotNull();

        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder
                .signal()
                .withName("Test")
                .withVariable("signalVar", "timeToGo")
                .build();

        //when
        signalRestTemplate.signal(signalProcessInstancesCmd);


        await("Broadcast Signals").untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            List<CloudBPMNSignalReceivedEvent> signalReceivedEvents = receivedEvents
                    .stream()
                    .filter(CloudBPMNSignalReceivedEvent.class::isInstance)
                    .map(CloudBPMNSignalReceivedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(signalReceivedEvents)
                    .filteredOn(event -> BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED.name().equals(event.getEventType().name()))
                    .extracting( CloudRuntimeEvent::getEventType,
                                 CloudRuntimeEvent::getProcessDefinitionId,
                                 CloudRuntimeEvent::getProcessInstanceId,
                                 CloudRuntimeEvent::getProcessDefinitionKey,
                                 CloudRuntimeEvent::getProcessDefinitionVersion,
                                 event -> event.getEntity().getProcessDefinitionId(),
                                 event -> event.getEntity().getProcessInstanceId(),
                                 event -> event.getEntity().getElementId(),
                                 event -> event.getEntity().getSignalPayload().getName(),
                                 event -> event.getEntity().getSignalPayload().getVariables()
                    )
                    .contains(
                            tuple(BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED,
                                  processWithSignalStart.getId(),
                                  null, // not available for start catch signal
                                  null, // not available for start catch signal
                                  null, // not available for start catch signal
                                  processWithSignalStart.getId(),
                                  null, // not available for start signal, should be checked once!
                                  "theStart",
                                  "Test",
                                  Collections.singletonMap("signalVar", "timeToGo")
                            ),
                            tuple(BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED,
                                  startProcessEntity1.getBody().getProcessDefinitionId(),
                                  startProcessEntity1.getBody().getId(),
                                  startProcessEntity1.getBody().getProcessDefinitionKey(),
                                  1, // version
                                  startProcessEntity1.getBody().getProcessDefinitionId(),
                                  startProcessEntity1.getBody().getId(),
                                  "signalintermediatecatchevent1",
                                  "Test",
                                  Collections.singletonMap("signalVar", "timeToGo")
                            ),
                            tuple(BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED,
                                  startProcessEntity2.getBody().getProcessDefinitionId(),
                                  startProcessEntity2.getBody().getId(),
                                  startProcessEntity2.getBody().getProcessDefinitionKey(),
                                  1, // version
                                  startProcessEntity2.getBody().getProcessDefinitionId(),
                                  startProcessEntity2.getBody().getId(),
                                  "signalintermediatecatchevent1",
                                  "Test",
                                  Collections.singletonMap("signalVar", "timeToGo")
                            )
                    );

        });

    }


}
