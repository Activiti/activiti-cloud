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

import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

public class OutboxIntegrationFlowFactory implements BiFunction<String, QueueChannel, IntegrationFlow> {

    private final OutboxProperties outboxProperties;
    private final PlatformTransactionManager transactionManager;
    private final ObjectProvider<StreamBridge> streamBridgeProvider;
    private final LockRegistry<? extends Lock> lockRegistry;
    private final Advice outboxMessageHandlerErrorAdvice;

    public OutboxIntegrationFlowFactory(
        OutboxProperties outboxProperties,
        PlatformTransactionManager transactionManager,
        ObjectProvider<StreamBridge> streamBridgeProvider,
        LockRegistry<? extends Lock> lockRegistry,
        Advice outboxMessageHandlerErrorAdvice
    ) {
        this.outboxProperties = outboxProperties;
        this.transactionManager = transactionManager;
        this.streamBridgeProvider = streamBridgeProvider;
        this.lockRegistry = lockRegistry;
        this.outboxMessageHandlerErrorAdvice = outboxMessageHandlerErrorAdvice;
    }

    @Override
    public IntegrationFlow apply(String partitionKey, QueueChannel queueChannel) {
        return new OutboxIntegrationFlow(
            partitionKey,
            queueChannel,
            outboxProperties,
            transactionManager,
            streamBridgeProvider,
            lockRegistry,
            outboxMessageHandlerErrorAdvice
        );
    }
}
