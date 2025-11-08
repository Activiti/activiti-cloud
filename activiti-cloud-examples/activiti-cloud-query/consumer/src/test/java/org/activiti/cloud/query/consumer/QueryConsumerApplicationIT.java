/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.test.liquibase.CleanupLiquibaseAfterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.DeclarableCustomizer;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(classes = { QueryConsumerApplication.class })
@CleanupLiquibaseAfterTest
@Import(QueryConsumerApplicationIT.BinderFactoryListenerConfiguration.class)
public class QueryConsumerApplicationIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withReuse(true);

    static final Map<String, Queue> queues = new LinkedHashMap<>();
    static final Map<String, AnonymousQueue> anonQueues = new LinkedHashMap<>();
    static final Map<String, Exchange> exchanges = new LinkedHashMap<>();

    @TestConfiguration
    static class BinderFactoryListenerConfiguration {

        @Bean
        DeclarableCustomizer declarableCustomizer() {
            return declarable -> {
                if (declarable instanceof AnonymousQueue queue) {
                    anonQueues.computeIfAbsent(queue.getName(), key -> queue);
                } else if (declarable instanceof Queue queue) {
                    queues.computeIfAbsent(queue.getName(), key -> queue);
                } else if (declarable instanceof Exchange exchange) {
                    exchanges.computeIfAbsent(exchange.getName(), key -> exchange);
                }

                return declarable;
            };
        }
    }

    @AfterAll
    static void cleanUp() {
        queues.clear();
        exchanges.clear();
        anonQueues.clear();
    }

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
        assertThat(queues).isNotEmpty().hasSize(2).containsOnlyKeys("engineEvents.query", "engineEvents.audit");
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(anonQueues).isEmpty();
    }

    @Test
    void rabbitExchanges() {
        assertThat(exchanges).isNotEmpty().containsOnlyKeys("engineEvents");
    }
}
