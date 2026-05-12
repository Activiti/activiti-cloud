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
package org.activiti.cloud.common.messaging.config.test;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.CONNECTOR_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.function.context.FunctionRegistration.REGISTRATION_NAME_SUFFIX;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "spring.cloud.stream.bindings.restConsumer.destination=rest.GET",
        "spring.cloud.stream.bindings.restConsumer.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.routes.restConsumer.enabled=true",
    }
)
@EnableTestBinder
@Import({ TestBindingsChannelsConfiguration.class })
public class FunctionRouterExecutorFactoryIT {

    private static final String REST_GET_HANDLER = "restGetHandler";
    private static final String EXPECTED_REGISTRATION_KEY = REST_GET_HANDLER + REGISTRATION_NAME_SUFFIX;

    private static final AtomicReference<String> lastFactoryKey = new AtomicReference<>();
    private static final AtomicInteger executorTaskCount = new AtomicInteger();
    private static final AtomicReference<String> receivedPayload = new AtomicReference<>();

    @Autowired
    private InputDestination input;

    @TestConfiguration
    static class ApplicationConfig {

        private final List<ExecutorService> createdExecutors = new ArrayList<>();

        @Bean(REST_GET_HANDLER)
        @ConnectorBinding(input = TestBindingsChannels.REST_CONSUMER, connectorType = "rest.GET", condition = "true")
        ConsumerConnector<String> restGetHandler() {
            return payload -> receivedPayload.set(payload);
        }

        @Bean
        Function<String, ExecutorService> functionRouterExecutorFactory() {
            return registrationKey -> {
                lastFactoryKey.set(registrationKey);
                ThreadFactory daemonThreadFactory = runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    return thread;
                };
                ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    daemonThreadFactory
                ) {
                    @Override
                    public void execute(Runnable command) {
                        executorTaskCount.incrementAndGet();
                        super.execute(command);
                    }
                };
                createdExecutors.add(executor);
                return executor;
            };
        }

        @PreDestroy
        void shutdownExecutors() {
            createdExecutors.forEach(ExecutorService::shutdown);
        }
    }

    @BeforeEach
    void setUp() {
        lastFactoryKey.set(null);
        executorTaskCount.set(0);
        receivedPayload.set(null);
    }

    @Test
    void shouldUseCustomExecutorWhenFunctionRouterExecutorFactoryPresent() {
        Message<String> message = MessageBuilder
            .withPayload("GET http://localhost:8080")
            .setHeader(CONNECTOR_TYPE, "rest.GET")
            .build();

        input.send(message, "rest.GET");

        await()
            .untilAsserted(() -> {
                assertThat(receivedPayload.get()).isEqualTo("GET http://localhost:8080");
                assertThat(lastFactoryKey.get()).isEqualTo(EXPECTED_REGISTRATION_KEY);
                assertThat(executorTaskCount.get()).isGreaterThan(0);
            });
    }
}
