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
package org.activiti.cloud.common.messaging.outbox;

import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "activiti.cloud.messaging.destination-transformers-enabled=false",
        "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
        "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.commandResults.destination=commandResults",
    },
    classes = { OutboxAutoConfigurationIT.ApplicationConfig.class }
)
@TestPropertySource(properties = { "activiti.cloud.messaging.outbox.enabled=true", "spring.sql.init.platform=h2" })
@EnableTestBinder
@SpringBootApplication
class OutboxAutoConfigurationIT {

    private static final String AUDIT_PRODUCER = "auditProducer";

    @Autowired
    private MessageChannel auditProducer;

    @Autowired
    private DirectChannel outboxExceptionsChannel;

    @Autowired
    private QueueChannel outboxErrorsChannel;

    @Autowired
    private QueueChannel auditProducerQueue;

    @Autowired
    private ChannelMessageStore outboxChannelMessageStore;

    @BeforeEach
    void setUp() {
        outboxChannelMessageStore.removeMessageGroup("outboxErrorMessages");
    }

    @TestConfiguration
    static class ApplicationConfig {

        @OutputBinding(AUDIT_PRODUCER)
        MessageChannel auditProducer() {
            return new DirectChannel();
        }
    }

    @Test
    void contextLoads() {}
}
