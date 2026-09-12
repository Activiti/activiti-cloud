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

/**
 * Binder-neutral signal that a message could not be delivered to its per-connector executor and
 * must be requeued for redelivery rather than reported to the connector as an application error.
 * Thrown by the executor's rejection handler when the executor is shutting down or the submitting
 * thread is interrupted; recognised as a delivery failure by the function router, which requeues
 * the message via whichever acknowledgment mechanism the active binder provides.
 */
public class RequeueDeliveryException extends RuntimeException {

    public RequeueDeliveryException(String message) {
        super(message);
    }

    public RequeueDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
