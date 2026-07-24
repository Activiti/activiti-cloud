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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.liquibase.EnableCleanupLiquibaseAfterTest;
import org.activiti.cloud.starters.test.binder.EnableBinderFactoryListenerTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(
    classes = {
        RuntimeBundleApplication.class, RuntimeBundleTransactedConnectorBindingIT.TestScriptExecuteConsumer.class,
    },
    properties = { "activiti.cloud.application.name=default-app", "activiti.cloud.messaging.rabbitmq.compress=false" }
)
@EnableCleanupLiquibaseAfterTest
@EnableBinderFactoryListenerTestContext
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@ResourceLocks(value = { @ResourceLock("rabbitmq"), @ResourceLock("postgres") })
class RuntimeBundleTransactedConnectorBindingIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final AtomicBoolean isReceived = new AtomicBoolean();
    private static final AtomicBoolean isSent = new AtomicBoolean();

    @Test
    void should_commitConnectorBindingTransactedChannel() {
        isReceived.set(false);
        isSent.set(false);

        transactionTemplate.executeWithoutResult(tx -> {
            isSent.set(streamBridge.send("script.EXECUTE", "println('foobar')"));
        });

        assertThat(isSent).isTrue();
        await().untilAsserted(() -> assertThat(isReceived).isTrue());
    }

    @Test
    void should_rollbackConnectorBindingTransactedChannel() {
        isReceived.set(false);
        isSent.set(false);

        transactionTemplate.executeWithoutResult(tx -> {
            isSent.set(streamBridge.send("script.EXECUTE", "println('foobar')"));

            await()
                .pollDelay(Duration.ofSeconds(1))
                .untilAtomic(isSent, value -> assertThat(value).isTrue());

            tx.setRollbackOnly();
        });

        assertThat(isSent).isTrue();
        await().untilAsserted(() -> assertThat(isReceived).isFalse());
    }

    @Test
    void should_rollbackBeforeCommitConnectorBindingTransactedChannel() {
        isReceived.set(false);
        isSent.set(false);

        assertThatThrownBy(() ->
            transactionTemplate.executeWithoutResult(tx -> {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void beforeCommit(boolean readOnly) {
                            isSent.set(streamBridge.send("script.EXECUTE", "println('foobar')"));

                            await()
                                .pollDelay(Duration.ofSeconds(1))
                                .untilAtomic(isSent, value -> assertThat(value).isTrue());

                            throw new RuntimeException("Boom before commit");
                        }
                    }
                );
            })
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Boom before commit");

        assertThat(isSent).isTrue();
        await().untilAsserted(() -> assertThat(isReceived).isFalse());
    }

    @EnableRabbit
    @Configuration(proxyBeanMethods = false)
    static class TestScriptExecuteConsumer {

        @RabbitListener(
            bindings = @QueueBinding(
                value = @Queue(name = "test-script-executor", autoDelete = "true"),
                exchange = @Exchange(name = "script.EXECUTE", type = "topic"),
                key = "#"
            )
        )
        void scriptExecutor(Message<String> message) {
            isReceived.set("println('foobar')".equals(message.getPayload()));
        }
    }
}
