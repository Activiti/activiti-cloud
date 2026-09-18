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

import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.messaging.MessageChannel;

public class IdempotentInterceptorBuilder {

    private final ConcurrentMetadataStore metadataStore;
    private MessageProcessor<String> keyStrategy;
    private MessageChannel discardChannel;
    private String discardChannelName;
    private boolean throwExceptionOnRejection = true;

    // Private constructor: enforce entry point
    private IdempotentInterceptorBuilder(ConcurrentMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    // Static entry point
    public static IdempotentInterceptorBuilder create(ConcurrentMetadataStore metadataStore) {
        return new IdempotentInterceptorBuilder(metadataStore);
    }

    // Define how to extract the deduplication key
    public IdempotentInterceptorBuilder keyStrategy(MessageProcessor<String> keyStrategy) {
        this.keyStrategy = keyStrategy;
        return this;
    }

    // Convenient shortcut to extract from a specific header name
    public IdempotentInterceptorBuilder keyFromHeader(String headerName) {
        this.keyStrategy = message -> message.getHeaders().get(headerName, String.class);
        return this;
    }

    // Set discard channel by object reference
    public IdempotentInterceptorBuilder discardTo(MessageChannel discardChannel) {
        this.discardChannel = discardChannel;
        this.throwExceptionOnRejection = false; // Turn off exceptions if discarding
        return this;
    }

    // Set discard channel by bean name
    public IdempotentInterceptorBuilder discardToChannelName(String discardChannelName) {
        this.discardChannelName = discardChannelName;
        this.throwExceptionOnRejection = false;
        return this;
    }

    // Explicitly toggle exception throwing behavior
    public IdempotentInterceptorBuilder throwExceptionOnRejection(boolean throwException) {
        this.throwExceptionOnRejection = throwException;
        return this;
    }

    // Terminal build method
    public IdempotentReceiverInterceptor build() {
        if (this.keyStrategy == null) {
            throw new IllegalStateException("A keyStrategy or keyFromHeader must be defined.");
        }

        MetadataStoreSelector selector = new MetadataStoreSelector(this.keyStrategy, this.metadataStore);
        IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(selector);

        if (this.discardChannel != null) {
            interceptor.setDiscardChannel(this.discardChannel);
        } else if (this.discardChannelName != null) {
            interceptor.setDiscardChannelName(this.discardChannelName);
        }

        interceptor.setThrowExceptionOnRejection(this.throwExceptionOnRejection);
        return interceptor;
    }
}
