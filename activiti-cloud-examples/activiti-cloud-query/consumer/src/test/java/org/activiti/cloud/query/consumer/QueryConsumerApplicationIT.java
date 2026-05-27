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
package org.activiti.cloud.query.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.test.liquibase.EnableCleanupLiquibaseAfterTest;
import org.activiti.cloud.starters.test.binder.BinderFactoryListenerTestContext;
import org.activiti.cloud.starters.test.binder.EnableBinderFactoryListenerTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(classes = { QueryConsumerApplication.class })
@EnableCleanupLiquibaseAfterTest
@EnableBinderFactoryListenerTestContext
@ResourceLocks(value = { @ResourceLock("postgres"), @ResourceLock("rabbitmq") })
public class QueryConsumerApplicationIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @Autowired
    protected BinderFactoryListenerTestContext binderFactoryListenerTestContext;

    @Autowired
    protected Environment environment;

    @Autowired
    protected ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    protected BindingServiceProperties bindingServiceProperties;

    @Test
    public void contextLoads() {}

    @Test
    void rabbitBinderCompression() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.binder.compression-level", Integer.class))
            .isEqualTo(9);
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.compress", Boolean.class))
            .isTrue();
    }

    @Test
    void messagingPropertiesRabbitMqCompression() {
        assertThat(messagingProperties.getRabbitmq().getCompressionLevel()).isEqualTo(9);
        assertThat(messagingProperties.getRabbitmq().isCompress()).isTrue();
    }

    @Test
    void messagingRabbitMqPrefixProperties() {
        assertThat(messagingProperties.getRabbitmq().getPrefix()).isNullOrEmpty();
    }

    @Test
    void rabbitBinderDefaultPrefix() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.consumer.prefix", String.class))
            .isNullOrEmpty();
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.prefix", String.class))
            .isNullOrEmpty();
    }

    @Test
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues())
            .isNotEmpty()
            .hasSize(2)
            .containsOnlyKeys("engineEvents.query", "engineEvents.audit");
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getAnonymousQueues()).isEmpty();
    }

    @Test
    void rabbitExchanges() {
        assertThat(binderFactoryListenerTestContext.getExchanges())
            .isNotEmpty()
            .containsOnlyKeys("engineEvents", "projectedEngineEventsProducer");
    }
}
