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
package org.activiti.cloud.conf;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.QueryConsumerChannels;
import org.activiti.cloud.services.query.app.QueryConsumerMessageHandler;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@EnableAsync
@Import(QueryConsumerChannelsConfiguration.class)
public class QueryConsumerAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryConsumerAutoConfiguration.class);
    public static final String PARTITIONED_QUERY_CONSUMER_INTEGRATION_FLOW_INPUT =
        "partitionedQueryConsumerIntegrationFlowInput";
    public static final String PARTITIONED_QUERY_CONSUMER_ERROR_CHANNEL = "partitionedQueryConsumerErrorChannel";

    @Bean
    InitializingBean queryConsumerAutoConfigurationInfo(
        QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider
    ) {
        return () -> {
            LOGGER.info(
                "Initializing QueryConsumerAutoConfiguration with {} partitioned channel count",
                queryConsumerPartitionedChannelCountProvider.get()
            );
        };
    }

    @Bean
    @FunctionBinding(input = QueryConsumerChannels.QUERY_CONSUMER)
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> queryConsumerFunction(
        IntegrationFlow partitionedQueryConsumerIntegrationFlow
    ) {
        return message -> partitionedQueryConsumerIntegrationFlow.getInputChannel().send(message);
    }

    @Bean
    @ConditionalOnMissingBean
    QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider() {
        return new RuntimeQueryConsumerPartitionedChannelCountProvider();
    }

    @Bean
    QueryConsumerPartitionedChannelKeySelector queryConsumerPartitionedChannelKeySelector() {
        return new DefaultConsumerPartitionedChannelKeySelector();
    }

    @Bean
    QueryConsumerMessageHandler queryConsumerMessageHandler(
        QueryEventHandlerContext eventHandlerContext,
        QueryEventHandlerContextOptimizer optimizer,
        EntityManager entityManager,
        IntegrationFlow queryEventsQueueIntegrationFlow
    ) {
        return new QueryConsumerMessageHandler(
            eventHandlerContext,
            optimizer,
            entityManager,
            queryEventsQueueIntegrationFlow.getInputChannel()
        );
    }

    @Bean
    public IntegrationFlow partitionedQueryConsumerErrorIntegrationFlow() {
        return IntegrationFlow
            .from(PARTITIONED_QUERY_CONSUMER_ERROR_CHANNEL)
            .handle(message -> {
                if (message instanceof ErrorMessage errorMessage) {
                    final var exception = errorMessage.getPayload();

                    LOGGER.error(
                        "{} while handling {} for partition thread {}",
                        exception.getMessage(),
                        errorMessage.getOriginalMessage(),
                        Thread.currentThread().getName(),
                        Optional.ofNullable(exception.getCause()).orElse(exception)
                    );
                } else {
                    LOGGER.error(
                        "Unexpected message type {} on {}: {}",
                        message.getClass(),
                        PARTITIONED_QUERY_CONSUMER_ERROR_CHANNEL,
                        message
                    );
                }
            })
            .get();
    }

    @Bean
    public IntegrationFlow partitionedQueryConsumerIntegrationFlow(
        QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider,
        QueryConsumerPartitionedChannelKeySelector queryConsumerPartitionedChannelKeySelector,
        GenericHandler<List<CloudRuntimeEvent<?, ?>>> genericQueryConsumerChannelHandlerAdapter,
        @Value("${activiti.cloud.query.consumer.worker-queue-size:10}") Integer workerQueueSize
    ) {
        return IntegrationFlow
            .from(PARTITIONED_QUERY_CONSUMER_INTEGRATION_FLOW_INPUT)
            .enrichHeaders(headers -> headers.errorChannel(PARTITIONED_QUERY_CONSUMER_ERROR_CHANNEL))
            .channel(
                MessageChannels
                    .partitioned(queryConsumerPartitionedChannelCountProvider.get())
                    .partitionKey(queryConsumerPartitionedChannelKeySelector)
                    .workerQueueSize(workerQueueSize)
            )
            .handle(genericQueryConsumerChannelHandlerAdapter, endpoint -> endpoint.requiresReply(false))
            .get();
    }

    @Bean
    GenericHandler<List<CloudRuntimeEvent<?, ?>>> genericQueryConsumerChannelHandlerAdapter(
        QueryConsumerMessageHandler queryConsumerMessageHandler
    ) {
        return (events, headers) -> {
            LOGGER.debug(
                "Handling {} events with root process instance id {} on partition thread: {}",
                events.size(),
                headers.get(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID),
                Thread.currentThread().getName()
            );

            queryConsumerMessageHandler.accept(MessageBuilder.withPayload(events).copyHeaders(headers).build());

            return null;
        };
    }

    @Bean
    @ConditionalOnMissingBean
    ChannelMessageStore queryEventsChannelMessageStore() {
        return new SimpleMessageStore();
    }

    @Bean
    IntegrationFlow queryEventsIntegrationFlow(
        QueueChannel queryEventsQueueChannel,
        MessageChannel queryEventsProducer,
        PlatformTransactionManager platformTransactionManager,
        Trigger queryEventsPollerTrigger
    ) {
        return IntegrationFlow
            .from(queryEventsQueueChannel)
            .log(LoggingHandler.Level.DEBUG)
            .handle(
                message -> queryEventsProducer.send(message),
                endpoint ->
                    endpoint.poller(Pollers.trigger(queryEventsPollerTrigger).transactional(platformTransactionManager))
            )
            .get();
    }

    @Bean
    @ConditionalOnMissingBean
    Trigger queryEventsPollerTrigger(QueueChannel queryEventsQueueChannel) {
        return new QueueSizeBasedTrigger(queryEventsQueueChannel, Duration.ofMillis(100), Duration.ZERO);
    }

    @Bean
    IntegrationFlow queryEventsQueueIntegrationFlow(
        QueueChannel queryEventsChannel,
        @Value(
            "${activit.cloud.query.consumer.events.queue.headers-to-remove:sourceData,errorChannel,replyChannel,amqp_*,spring.cloud.function.definition}"
        ) String[] headersToRemove
    ) {
        return IntegrationFlow
            .from("queryEventsQueueIntegrationFlowInput")
            .headerFilter(headersToRemove)
            .channel(queryEventsChannel)
            .get();
    }
}
