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

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptorFactory.OUTBOX_ROLE;
import static org.springframework.messaging.MessageHeaders.ERROR_CHANNEL;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.Lock;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.aop.ReceiveMessageAdvice;
import org.springframework.integration.aop.SimpleActiveIdleReceiveMessageAdvice;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.MessageHandler;
import org.springframework.transaction.PlatformTransactionManager;

public class OutboxIntegrationFlow extends IntegrationFlowAdapter {

    private final String partitionKey;
    private final QueueChannel queueChannel;
    private final OutboxProperties outboxProperties;
    private final PlatformTransactionManager transactionManager;
    private final ObjectProvider<StreamBridge> streamBridgeProvider;
    private final LockRegistry<? extends Lock> lockRegistry;
    private final AbstractRequestHandlerAdvice[] outboxMessageHandlerErrorAdvice;
    private final OutboxTaskScheduler taskScheduler;

    public OutboxIntegrationFlow(
        String partitionKey,
        QueueChannel queueChannel,
        OutboxProperties outboxProperties,
        PlatformTransactionManager transactionManager,
        ObjectProvider<StreamBridge> streamBridgeProvider,
        LockRegistry<? extends Lock> lockRegistry,
        List<AbstractRequestHandlerAdvice> outboxMessageHandlerErrorAdvice
    ) {
        this.partitionKey = partitionKey;
        this.queueChannel = queueChannel;
        this.outboxProperties = outboxProperties;
        this.transactionManager = transactionManager;
        this.streamBridgeProvider = streamBridgeProvider;
        this.lockRegistry = lockRegistry;
        this.outboxMessageHandlerErrorAdvice = outboxMessageHandlerErrorAdvice.toArray(
            AbstractRequestHandlerAdvice[]::new
        );
        this.taskScheduler = new OutboxTaskScheduler(partitionKey);
    }

    @Override
    protected @NonNull IntegrationFlowDefinition<?> buildFlow() {
        // 1. Isolate the messages by using the partition key for queue messages group
        final var dynamicPeriodicTrigger = new DynamicPeriodicTrigger(outboxProperties.getPollerActivePeriod());

        // 2. Build a single-threaded flow dedicated to this partition
        return IntegrationFlow.from(queueChannel)
            .bridge(endpoint ->
                endpoint
                    .id(partitionKey.concat(OUTBOX_ROLE))
                    .role(OUTBOX_ROLE)
                    .autoStartup(false)
                    .taskScheduler(taskScheduler)
                    .poller(
                        Pollers.trigger(dynamicPeriodicTrigger)
                            .maxMessagesPerPoll(1)
                            .receiveTimeout(Duration.ZERO.toMillis())
                            .errorChannel(ERROR_CHANNEL)
                            .advice(
                                pollCircuitBreakerAdvice(),
                                pollLockAdvice(partitionKey),
                                pollActiveIdleAdvice(dynamicPeriodicTrigger)
                            )
                            .transactional(transactionManager)
                    )
            )
            // Strict DB transaction boundary per loop
            .handle(outboxMessageHandler(partitionKey), endpoint -> endpoint.advice(outboxMessageHandlerErrorAdvice));
    }

    private MessageHandler outboxMessageHandler(String partitionKey) {
        return new OutboxMessageHandler(partitionKey, streamBridgeProvider);
    }

    private PollLockAdvice pollLockAdvice(String partitionKey) {
        return new PollLockAdvice(lockRegistry, partitionKey);
    }

    public CircuitBreakerPollerAdvice pollCircuitBreakerAdvice() {
        return new CircuitBreakerPollerAdvice(
            outboxProperties.getCircuitBreakerThreshold(),
            outboxProperties.getCircuitBreakerOpenPeriod()
        );
    }

    private ReceiveMessageAdvice pollActiveIdleAdvice(DynamicPeriodicTrigger trigger) {
        final var advice = new SimpleActiveIdleReceiveMessageAdvice(trigger);
        advice.setActivePollPeriod(outboxProperties.getPollerActivePeriod().toMillis());
        advice.setIdlePollPeriod(outboxProperties.getPollerIdlePeriod().toMillis());

        return advice;
    }

    @Override
    public void stop() {
        super.stop();

        taskScheduler.close();
    }
}
