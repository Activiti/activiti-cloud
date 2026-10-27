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
package org.activiti.cloud.common.messaging.config.outbox;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import org.activiti.spring.AbstractActivitiSmartLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

public class OutboxChannelInterceptorFactory
    extends AbstractActivitiSmartLifeCycle
    implements BiFunction<String, BiFunction<Message<?>, DirectChannel, String>, ChannelInterceptor>
{

    public static final String OUTBOX_ROLE = "Outbox";

    private static final Logger log = LoggerFactory.getLogger(OutboxChannelInterceptorFactory.class);

    private final ChannelMessageStore channelMessageStore;
    private final PlatformTransactionManager transactionManager;
    private final IntegrationFlowContext integrationFlowContext;
    private final ObjectProvider<StreamBridge> streamBridgeProvider;
    private final LockRegistry lockRegistry;

    private final Map<String, ChannelInterceptor> interceptorsMap = new ConcurrentHashMap<>();
    private final Map<String, IntegrationFlowRegistration> flowRegistrationsMap = new ConcurrentHashMap<>();

    public OutboxChannelInterceptorFactory(
        ChannelMessageStore channelMessageStore,
        PlatformTransactionManager transactionManager,
        IntegrationFlowContext integrationFlowContext,
        ObjectProvider<StreamBridge> streamBridgeProvider,
        LockRegistry lockRegistry
    ) {
        this.channelMessageStore = channelMessageStore;
        this.transactionManager = transactionManager;
        this.integrationFlowContext = integrationFlowContext;
        this.streamBridgeProvider = streamBridgeProvider;
        this.lockRegistry = lockRegistry;
    }

    @Override
    public void doStart() {
        log.info("Start outbox partitions: {} ", flowRegistrationsMap.keySet());
        flowRegistrationsMap.values().forEach(IntegrationFlowRegistration::start);
    }

    @Override
    public void doStop() {
        log.info("Stop outbox partitions: {} ", flowRegistrationsMap.keySet());
        flowRegistrationsMap.values().forEach(IntegrationFlowRegistration::stop);
    }

    @Override
    public ChannelInterceptor apply(
        String partitionKey,
        BiFunction<Message<?>, DirectChannel, String> destinationProvider
    ) {
        return interceptorsMap.computeIfAbsent(partitionKey, _ -> {
            // 1. Isolate the messages by using the partition key for queue messages group
            final QueueChannel queueChannel = MessageChannels.queue(
                partitionKey.concat("Queue"),
                channelMessageStore,
                partitionKey.concat(OUTBOX_ROLE)
            ).getObject();

            // 2. Build a single-threaded flow dedicated to this partition
            IntegrationFlow partitionFlow = IntegrationFlow.from(queueChannel)
                // Strict DB transaction boundary per loop
                .handle(new OutboxMessageHandler(partitionKey, streamBridgeProvider), endpoint ->
                    endpoint
                        .id(partitionKey.concat(OUTBOX_ROLE))
                        .role(OUTBOX_ROLE)
                        .autoStartup(false)
                        .taskScheduler(taskScheduler(partitionKey))
                        .poller(
                            Pollers.fixedDelay(Duration.ofMillis(50))
                                .maxMessagesPerPoll(10)
                                .receiveTimeout(Duration.ofSeconds(1).toMillis())
                                .advice(new PollLockAdvice(lockRegistry, partitionKey))
                                .transactional(transactionManager)
                        )
                )
                .get();

            flowRegistrationsMap.put(
                partitionKey,
                integrationFlowContext
                    .registration(partitionFlow)
                    .id(partitionKey.concat("OutboxFlow"))
                    .autoStartup(false)
                    .register()
            );

            return new OutboxChannelInterceptor(queueChannel, destinationProvider);
        });
    }

    private TaskScheduler taskScheduler(String partitionKey) {
        return new ConcurrentTaskScheduler(
            Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name(partitionKey.concat("Poller")).factory()
            )
        );
    }
}
