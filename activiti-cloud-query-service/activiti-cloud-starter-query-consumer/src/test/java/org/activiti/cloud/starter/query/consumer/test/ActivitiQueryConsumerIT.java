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
package org.activiti.cloud.starter.query.consumer.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.config.PartitionedChannelGracefulShutdown;
import org.activiti.cloud.conf.FixedQueryConsumerPartitionedChannelCountProvider;
import org.activiti.cloud.conf.QueryConsumerPartitionedChannelCountProvider;
import org.activiti.cloud.conf.QueryConsumerPartitionedChannelKeySelector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "activiti.cloud.services.oauth2.iam-name=test", "activiti.cloud.query.consumer.shutdown-timeout=30s",
    }
)
@EnableTestBinder
@Import(ActivitiQueryConsumerIT.ControllableHandlerConfiguration.class)
class ActivitiQueryConsumerIT {

    @Autowired
    private QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider;

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    @Qualifier("partitionedQueryConsumerIntegrationFlow")
    private IntegrationFlow partitionedQueryConsumerIntegrationFlow;

    @Autowired
    private PartitionedChannelGracefulShutdown queryConsumerGracefulShutdown;

    @Autowired
    private ControllableHandler genericQueryConsumerChannelHandlerAdapter;

    @Test
    void contextLoads() {
        assertThat(queryConsumerPartitionedChannelCountProvider)
            .isInstanceOf(FixedQueryConsumerPartitionedChannelCountProvider.class)
            .extracting(Supplier::get)
            .isEqualTo(hikariDataSource.getMaximumPoolSize());
    }

    @Test
    void should_waitForInFlightEventToComplete_when_shuttingDown() throws Exception {
        final CountDownLatch eventStarted = new CountDownLatch(1);
        final CountDownLatch releaseEvent = new CountDownLatch(1);
        final AtomicBoolean eventCompleted = new AtomicBoolean(false);

        genericQueryConsumerChannelHandlerAdapter.setDelegate((events, headers) -> {
            eventStarted.countDown();
            try {
                if (!releaseEvent.await(30, TimeUnit.SECONDS)) throw new IllegalStateException(
                    "Timed out waiting for test to release event"
                );
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
            eventCompleted.set(true);
            return null;
        });

        partitionedQueryConsumerIntegrationFlow.getInputChannel().send(eventMessage("pi-1"));

        assertThat(eventStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(queryConsumerGracefulShutdown.inFlight()).isEqualTo(1);

        final Thread shutdownThread = new Thread(queryConsumerGracefulShutdown::stop, "graceful-shutdown-test");
        shutdownThread.setDaemon(true);
        shutdownThread.start();

        shutdownThread.join(TimeUnit.SECONDS.toMillis(2));
        assertThat(shutdownThread.isAlive()).isTrue();
        assertThat(eventCompleted).isFalse();

        releaseEvent.countDown();

        shutdownThread.join(TimeUnit.SECONDS.toMillis(10));
        assertThat(shutdownThread.isAlive()).isFalse();
        assertThat(eventCompleted).isTrue();
        assertThat(queryConsumerGracefulShutdown.inFlight()).isZero();
    }

    private static Message<List<CloudRuntimeEvent<?, ?>>> eventMessage(Object partitionKey) {
        return MessageBuilder.withPayload(List.<CloudRuntimeEvent<?, ?>>of())
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, partitionKey)
            .build();
    }

    static class ControllableHandler implements GenericHandler<List<CloudRuntimeEvent<?, ?>>> {

        private volatile GenericHandler<List<CloudRuntimeEvent<?, ?>>> delegate = (events, headers) -> null;

        void setDelegate(GenericHandler<List<CloudRuntimeEvent<?, ?>>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object handle(List<CloudRuntimeEvent<?, ?>> events, MessageHeaders headers) {
            return delegate.handle(events, headers);
        }
    }

    @TestConfiguration
    static class ControllableHandlerConfiguration {

        @Bean
        @Primary
        ControllableHandler testGenericQueryConsumerChannelHandlerAdapter() {
            return new ControllableHandler();
        }
    }
}
