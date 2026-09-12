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
import org.springframework.messaging.Message;

/**
 * Binder-specific capability the function router relies on to confirm or redeliver an individual
 * message once its asynchronous processing has completed. Kept behind this interface so the router
 * itself stays binder-agnostic: the concrete implementation is selected by which binder is on the
 * classpath (see {@code FunctionRouterConfiguration}), and can be overridden by declaring a
 * {@code DeliveryAcknowledgment} bean.
 */
public interface DeliveryAcknowledgment {
    /**
     * Confirms the message so the broker does not redeliver it. A no-op when the message carries no
     * acknowledgment handle, so it is safe to call regardless of ack mode.
     */
    void acknowledge(Message<?> message);

    /**
     * Negatively acknowledges the message with requeue so the broker redelivers it. A no-op when the
     * message carries no acknowledgment handle.
     */
    void requeue(Message<?> message);

    /**
     * Header names that carry the (typically live, non-serializable) acknowledgment handle. The
     * router strips these before forwarding the routed message to a connector or re-publishing it,
     * so the handle never leaks into a business message that a downstream handler may persist.
     */
    default Collection<String> acknowledgmentHeaders() {
        return List.of();
    }
}
