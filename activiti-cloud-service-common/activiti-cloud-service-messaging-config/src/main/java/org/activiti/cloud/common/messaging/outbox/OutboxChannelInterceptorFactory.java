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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class OutboxChannelInterceptorFactory
    implements
        BiFunction<String, BiFunction<Message<?>, MessageChannel, String>, OutboxChannelInterceptor>,
        SmartLifecycle
{

    public static final String OUTBOX_ROLE = "Outbox";

    private static final Logger log = LoggerFactory.getLogger(OutboxChannelInterceptorFactory.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final OutboxProperties outboxProperties;
    private final ChannelMessageStore channelMessageStore;
    private final IntegrationFlowContext integrationFlowContext;
    private final OutboxIntegrationFlowFactory outboxIntegrationFlowFactory;

    private final Map<String, OutboxChannelInterceptor> interceptorsMap = new ConcurrentHashMap<>();
    private final Map<String, IntegrationFlowRegistration> flowRegistrationsMap = new ConcurrentHashMap<>();

    public OutboxChannelInterceptorFactory(
        OutboxProperties outboxProperties,
        ChannelMessageStore channelMessageStore,
        OutboxIntegrationFlowFactory outboxIntegrationFlowFactory,
        IntegrationFlowContext integrationFlowContext
    ) {
        this.outboxProperties = outboxProperties;
        this.channelMessageStore = channelMessageStore;
        this.outboxIntegrationFlowFactory = outboxIntegrationFlowFactory;
        this.integrationFlowContext = integrationFlowContext;
    }

    @Override
    public OutboxChannelInterceptor apply(
        String partitionKey,
        BiFunction<Message<?>, MessageChannel, String> destinationProvider
    ) {
        return interceptorsMap.computeIfAbsent(partitionKey, _ -> {
            final var queueChannel = queueChannel(partitionKey);
            final var outboxIntegrationFlow = outboxIntegrationFlow(partitionKey, queueChannel);
            final var outboxIntegrationFlowRegistration = integrationFlowContext
                .registration(outboxIntegrationFlow)
                .id(partitionKey.concat("OutboxFlow"))
                .autoStartup(false)
                .register();

            flowRegistrationsMap.put(partitionKey.concat(OUTBOX_ROLE), outboxIntegrationFlowRegistration);

            return new OutboxChannelInterceptor(queueChannel, destinationProvider).withHeadersToRemove(
                outboxProperties.getHeadersToRemove()
            );
        });
    }

    private QueueChannel queueChannel(String partitionKey) {
        return MessageChannels.queue(
            partitionKey.concat("Queue"),
            channelMessageStore,
            partitionKey.concat(OUTBOX_ROLE)
        ).getObject();
    }

    private IntegrationFlow outboxIntegrationFlow(String partitionKey, QueueChannel queueChannel) {
        return outboxIntegrationFlowFactory.apply(partitionKey, queueChannel);
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("Registered outbox {} partitions properties {} ", flowRegistrationsMap.keySet(), outboxProperties);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stop outbox partitions: {} ", flowRegistrationsMap.keySet());
            flowRegistrationsMap.values().forEach(IntegrationFlowRegistration::stop);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
