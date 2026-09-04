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
package org.activiti.cloud.services.query.subscription;

/**
 * Type discriminator for {@link SubscriberRegistryMessage}, the fan-out contract that carries
 * subscriber presence from each query-rest instance to the query-consumer registry.
 */
public enum RegistryMessageType {
    /** A user gained their first live socket on the sending instance. Carries {@code userId} and {@code groups}. */
    REGISTERED,

    /** A user lost their last live socket on the sending instance. Carries {@code userId}. */
    UNREGISTERED,

    /** Broadcast by the consumer on startup, asking every instance to replay its local registry as a SNAPSHOT. */
    RESYNC_REQUEST,

    /** An instance's full local registry, sent in reply to a RESYNC_REQUEST. Carries {@code entries}. */
    SNAPSHOT,

    /** Periodic liveness signal from an instance, carrying only its {@code sourceId}. */
    HEARTBEAT,
}
