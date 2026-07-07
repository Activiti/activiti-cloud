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
package org.activiti.cloud.services.audit.jpa.streams.config;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannelHandler;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannels;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.streams.AuditConsumerChannelHandlerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

@AutoConfiguration
public class AuditJPAStreamsAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditJPAStreamsAutoConfiguration.class);
    public static final String PARTITIONED_AUDIT_CONSUMER_INTEGRATION_FLOW_INPUT =
        "partitionedAuditConsumerIntegrationFlowInput";
    public static final String PARTITIONED_AUDIT_CONSUMER_ERROR_CHANNEL = "partitionedAuditConsumerErrorChannel";

    @Bean
    @ConditionalOnMissingBean
    public AuditConsumerChannelHandler auditConsumerChannelHandler(
        EventsRepository eventsRepository,
        APIEventToEntityConverters eventConverters
    ) {
        return new AuditConsumerChannelHandlerImpl(eventsRepository, eventConverters);
    }

    @FunctionBinding(input = AuditConsumerChannels.AUDIT_CONSUMER)
    @Bean
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> auditConsumerChannelHandlerConsumer(
        IntegrationFlow partitionedAuditConsumerIntegrationFlow
    ) {
        return message -> partitionedAuditConsumerIntegrationFlow.getInputChannel().send(message);
    }

    @Bean
    public IntegrationFlow partitionedAuditConsumerIntegrationFlow(
        AuditConsumerPartitionedChannelCountProvider auditConsumerPartitionedChannelCountProvider,
        AuditConsumerPartitionedChannelKeySelector auditConsumerPartitionedChannelKeySelector,
        GenericHandler<List<CloudRuntimeEvent<?, ?>>> genericAuditConsumerChannelHandlerAdapter,
        @Value("${activiti.cloud.query.consumer.worker-queue-size:10}") Integer workerQueueSize
    ) {
        LOGGER.info(
            "Initializing AuditJPAStreamsAutoConfiguration with {} partitioned channel count using worker-queue size {}",
            auditConsumerPartitionedChannelCountProvider.get(),
            workerQueueSize
        );
        return IntegrationFlow.from(PARTITIONED_AUDIT_CONSUMER_INTEGRATION_FLOW_INPUT)
            .gateway(
                request ->
                    request
                        .enrichHeaders(headers -> headers.errorChannel(PARTITIONED_AUDIT_CONSUMER_ERROR_CHANNEL, true))
                        .channel(
                            MessageChannels.partitioned(auditConsumerPartitionedChannelCountProvider.get())
                                .partitionKey(auditConsumerPartitionedChannelKeySelector)
                                .workerQueueSize(workerQueueSize)
                        )
                        .handle(genericAuditConsumerChannelHandlerAdapter),
                gatewayEndpointSpec -> gatewayEndpointSpec.requiresReply(false).replyTimeout(0L)
            )
            .get();
    }

    @Bean
    public IntegrationFlow partitionedAudutConsumerErrorIntegrationFlow() {
        return IntegrationFlow.from(PARTITIONED_AUDIT_CONSUMER_ERROR_CHANNEL)
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
                        PARTITIONED_AUDIT_CONSUMER_ERROR_CHANNEL,
                        message
                    );
                }
            })
            .get();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditConsumerPartitionedChannelCountProvider auditConsumerPartitionedChannelCountProvider() {
        return () -> Runtime.getRuntime().availableProcessors() * 2;
    }

    @Bean
    AuditConsumerPartitionedChannelKeySelector auditConsumerPartitionedChannelKeySelector() {
        return new DefaultConsumerPartitionedChannelKeySelector();
    }

    @Bean
    GenericHandler<List<CloudRuntimeEvent<?, ?>>> genericAuditConsumerChannelHandlerAdapter(
        AuditConsumerChannelHandler auditConsumerChannelHandler
    ) {
        return (events, headers) -> {
            LOGGER.debug(
                "Handling {} events with root process instance id {} on partition thread: {}",
                events.size(),
                headers.get(AuditConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID),
                Thread.currentThread().getName()
            );

            auditConsumerChannelHandler.receiveCloudRuntimeEvent(
                headers,
                Optional.of(events).orElse(Collections.emptyList()).toArray(CloudRuntimeEvent[]::new)
            );

            return null;
        };
    }
}
