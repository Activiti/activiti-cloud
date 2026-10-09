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
package org.activiti.cloud.common.messaging.spring.integration;

import java.util.Optional;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.messaging.MessageChannel;

/**
 * A fluent custom builder for BarrierMessageHandler.
 */
public final class BarrierMessageHandlerBuilder {

    private final long requestTimeout;
    private Long triggerTimeout;
    private CorrelationStrategy correlationStrategy;
    private MessageGroupProcessor outputProcessor;
    private MessageChannel discardChannel;
    private String componentName;
    private Boolean async;

    // Enforce creation via static factory method
    private BarrierMessageHandlerBuilder(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Initializes the builder with the required timeout.
     * @param requestTimeout time in milliseconds
     */
    public static BarrierMessageHandlerBuilder create(long requestTimeout) {
        return new BarrierMessageHandlerBuilder(requestTimeout);
    }

    public BarrierMessageHandlerBuilder triggerTimeout(long triggerTimeout) {
        this.triggerTimeout = triggerTimeout;

        return this;
    }

    public BarrierMessageHandlerBuilder correlationStrategy(CorrelationStrategy strategy) {
        this.correlationStrategy = strategy;
        return this;
    }

    public BarrierMessageHandlerBuilder correlationHeader(String headerName) {
        this.correlationStrategy = new HeaderAttributeCorrelationStrategy(headerName);
        return this;
    }

    public BarrierMessageHandlerBuilder outputProcessor(MessageGroupProcessor processor) {
        this.outputProcessor = processor;
        return this;
    }

    public BarrierMessageHandlerBuilder discardChannel(MessageChannel discardChannel) {
        this.discardChannel = discardChannel;
        return this;
    }

    public BarrierMessageHandlerBuilder componentName(String componentName) {
        this.componentName = componentName;
        return this;
    }

    public BarrierMessageHandlerBuilder async(boolean async) {
        this.async = async;
        return this;
    }

    /**
     * Constructs and configures the final BarrierMessageHandler.
     */
    public BarrierMessageHandler build() {
        BarrierMessageHandler handler = new BarrierMessageHandler(
            this.requestTimeout,
            Optional.ofNullable(this.triggerTimeout).orElse(this.requestTimeout),
            Optional.ofNullable(this.outputProcessor).orElseGet(DefaultAggregatingMessageGroupProcessor::new),
            Optional.ofNullable(this.correlationStrategy).orElse(
                new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID)
            )
        );

        Optional.ofNullable(this.discardChannel).ifPresent(handler::setDiscardChannel);
        Optional.ofNullable(this.componentName).ifPresent(handler::setComponentName);
        Optional.ofNullable(this.async).ifPresent(handler::setAsync);

        return handler;
    }
}
