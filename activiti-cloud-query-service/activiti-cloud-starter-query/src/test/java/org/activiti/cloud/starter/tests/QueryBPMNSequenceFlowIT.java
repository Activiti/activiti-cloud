/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import java.util.UUID;
import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
class QueryBPMNSequenceFlowIT {

    private static final String TESTUSER = "testuser";

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    @Autowired
    private MyProducer producer;

    private EventsAggregator eventsAggregator;

    @BeforeEach
    void setUp() {
        eventsAggregator = new EventsAggregator(producer);
        identityTokenProducer.withTestUser(TESTUSER);
    }

    @AfterEach
    void tearDown() {
        bpmnSequenceFlowRepository.deleteAll();
    }

    @Test
    void should_truncateTaskNameAndDescription_when_theyAreTooLong() {
        CloudSequenceFlowTakenEventImpl sequenceFlowTakenEvent = buildSequenceFlowTakenEvent(
            "a".repeat(256),
            "a".repeat(256)
        );
        eventsAggregator.addEvents(sequenceFlowTakenEvent);
        eventsAggregator.sendAll();

        await().untilAsserted(() ->
            assertThat(bpmnSequenceFlowRepository.findByEventId(sequenceFlowTakenEvent.getId()))
                .isNotNull()
                .extracting(BPMNSequenceFlow::getSourceActivityName, BPMNSequenceFlow::getTargetActivityName)
                .contains("a".repeat(255), "a".repeat(255))
        );
    }

    private CloudSequenceFlowTakenEventImpl buildSequenceFlowTakenEvent(
        String sourceActivityName,
        String targetActivityName
    ) {
        BPMNSequenceFlow sequenceFlow = buildSequenceFlow(sourceActivityName, targetActivityName);

        return new CloudSequenceFlowTakenEventImpl(sequenceFlow);
    }

    private BPMNSequenceFlowImpl buildSequenceFlow(String sourceActivityName, String targetActivityName) {
        BPMNSequenceFlowImpl sequenceFlow = buildSequenceFlow();
        sequenceFlow.setSourceActivityName(sourceActivityName);
        sequenceFlow.setTargetActivityName(targetActivityName);
        return sequenceFlow;
    }

    private BPMNSequenceFlowImpl buildSequenceFlow() {
        return new BPMNSequenceFlowImpl(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
    }
}
