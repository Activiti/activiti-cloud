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

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Fan-out message broadcast by each query-rest instance on the registry channel and merged by the
 * query-consumer into its {@code ConsumerSubscriberRegistry}. Which fields are populated depends on
 * {@link #type()}; use the static factories, which are the single supported way to build each
 * variant and encode the field-presence rules of the contract.
 *
 * <p>{@code sourceId} identifies the sending instance: it drives instance-liveness expiry on the
 * consumer and lets a sender ignore its own message in a joint (single-process) deployment.
 */
public record SubscriberRegistryMessage(
    RegistryMessageType type,
    String userId,
    List<String> groups,
    List<Entry> entries,
    String sourceId,
    Instant sentAt
) {
    /** A single user's presence within a {@link RegistryMessageType#SNAPSHOT}. */
    public record Entry(String userId, List<String> groups) {
        public Entry {
            Objects.requireNonNull(userId, "userId");
            groups = groups == null ? List.of() : List.copyOf(groups);
        }
    }

    public SubscriberRegistryMessage {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(sentAt, "sentAt");
        groups = groups == null ? null : List.copyOf(groups);
        entries = entries == null ? null : List.copyOf(entries);
    }

    public static SubscriberRegistryMessage registered(
        String userId,
        List<String> groups,
        String sourceId,
        Instant sentAt
    ) {
        Objects.requireNonNull(userId, "userId");
        return new SubscriberRegistryMessage(
            RegistryMessageType.REGISTERED,
            userId,
            groups == null ? List.of() : groups,
            null,
            sourceId,
            sentAt
        );
    }

    public static SubscriberRegistryMessage unregistered(String userId, String sourceId, Instant sentAt) {
        Objects.requireNonNull(userId, "userId");
        return new SubscriberRegistryMessage(RegistryMessageType.UNREGISTERED, userId, null, null, sourceId, sentAt);
    }

    public static SubscriberRegistryMessage heartbeat(String sourceId, Instant sentAt) {
        return new SubscriberRegistryMessage(RegistryMessageType.HEARTBEAT, null, null, null, sourceId, sentAt);
    }

    public static SubscriberRegistryMessage resyncRequest(String sourceId, Instant sentAt) {
        return new SubscriberRegistryMessage(RegistryMessageType.RESYNC_REQUEST, null, null, null, sourceId, sentAt);
    }

    public static SubscriberRegistryMessage snapshot(List<Entry> entries, String sourceId, Instant sentAt) {
        Objects.requireNonNull(entries, "entries");
        return new SubscriberRegistryMessage(RegistryMessageType.SNAPSHOT, null, null, entries, sourceId, sentAt);
    }
}
