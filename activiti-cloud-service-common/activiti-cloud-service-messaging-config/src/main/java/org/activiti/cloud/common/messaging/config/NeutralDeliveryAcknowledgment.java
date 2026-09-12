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
package org.activiti.cloud.common.messaging.config;

import java.util.Collection;
import java.util.List;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.messaging.Message;

/**
 * Binder-neutral {@link DeliveryAcknowledgment} backed solely by Spring Integration's
 * {@link AcknowledgmentCallback}. Used for binders that populate that callback (e.g. Kafka); the
 * fallback when no binder-specific implementation is on the classpath.
 */
class NeutralDeliveryAcknowledgment implements DeliveryAcknowledgment {

    @Override
    public void acknowledge(Message<?> message) {
        acknowledge(message, AcknowledgmentCallback.Status.ACCEPT);
    }

    @Override
    public void requeue(Message<?> message) {
        acknowledge(message, AcknowledgmentCallback.Status.REQUEUE);
    }

    @Override
    public Collection<String> acknowledgmentHeaders() {
        return List.of(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);
    }

    private static void acknowledge(Message<?> message, AcknowledgmentCallback.Status status) {
        final var callback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
        if (callback != null) {
            callback.acknowledge(status);
        }
    }
}
