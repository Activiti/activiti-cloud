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
package org.activiti.cloud.starter.audit.tests.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.UUID;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
class AuditCommandIdIT {

    @Autowired
    private EventsRepository repository;

    @Autowired
    private MyProducer producer;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void should_persistCommandId_when_eventWithCommandIdIsReceived() {
        var commandId = UUID.randomUUID().toString();
        var processInstance = new ProcessInstanceImpl();
        processInstance.setId("proc-inst-" + UUID.randomUUID());

        var event = new CloudProcessStartedEventImpl(processInstance);
        event.setCommandId(commandId);
        event.setAppName("test-app");
        event.setServiceName("test-rb");

        producer.send(event);

        await()
            .untilAsserted(() -> {
                var entities = repository.findAllByOrderByTimestampDesc();
                assertThat(entities)
                    .hasSize(1)
                    .first()
                    .satisfies(entity -> assertThat(((AuditEventEntity) entity).getCommandId()).isEqualTo(commandId));
            });
    }

    @Test
    void should_persistNullCommandId_when_eventWithoutCommandIdIsReceived() {
        var processInstance = new ProcessInstanceImpl();
        processInstance.setId("proc-inst-" + UUID.randomUUID());

        var event = new CloudProcessStartedEventImpl(processInstance);
        event.setAppName("test-app");
        event.setServiceName("test-rb");

        producer.send(event);

        await()
            .untilAsserted(() -> {
                var entities = repository.findAllByOrderByTimestampDesc();
                assertThat(entities)
                    .hasSize(1)
                    .first()
                    .satisfies(entity -> assertThat(((AuditEventEntity) entity).getCommandId()).isNull());
            });
    }

    @Test
    void should_persistSharedCommandId_when_multipleEventsShareTheSameCommandId() {
        var commandId = UUID.randomUUID().toString();

        var processInstance = new ProcessInstanceImpl();
        processInstance.setId("proc-inst-" + UUID.randomUUID());

        var firstEvent = new CloudProcessStartedEventImpl(processInstance);
        firstEvent.setCommandId(commandId);
        firstEvent.setAppName("test-app");
        firstEvent.setServiceName("test-rb");

        var secondEvent = new CloudProcessStartedEventImpl(processInstance);
        secondEvent.setCommandId(commandId);
        secondEvent.setAppName("test-app");
        secondEvent.setServiceName("test-rb");

        producer.send(firstEvent, secondEvent);

        await()
            .untilAsserted(() -> {
                var entities = repository.findAllByOrderByTimestampDesc();
                assertThat(entities)
                    .hasSize(2)
                    .allSatisfy(entity -> assertThat(((AuditEventEntity) entity).getCommandId()).isEqualTo(commandId));
            });
    }
}
