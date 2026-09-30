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
package org.activiti.cloud.services.query.app;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer-side view of who is watching, merged from every query-rest instance's presence broadcasts
 * (who and which groups, never socket counts). A user is kept while at least one instance still holds
 * them. An instance's messages can arrive out of order, so its events are ordered per user by their
 * {@code sentAt}: a stale resync snapshot can't re-add a user a later {@code UNREGISTERED} removed.
 * All mutating operations are synchronized.
 */
public class ConsumerSubscriberRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerSubscriberRegistry.class);

    private static final class Subscriber {

        private Set<String> groups;
        private final Set<String> sources = new HashSet<>();

        Subscriber(Set<String> groups) {
            this.groups = groups;
        }
    }

    private final Map<String, Subscriber> registry = new HashMap<>();
    private final Map<String, Instant> lastSeenBySource = new HashMap<>();
    // Per source, each user's last membership-event time; kept briefly after drop to reject stale re-adds.
    private final Map<String, Map<String, Instant>> lastEventBySourceUser = new HashMap<>();

    /**
     * Records a live subscription for {@code userId} on {@code sourceId}, refreshing groups and liveness.
     *
     * @return {@code true} if the user became watched for the first time (empty holder set to non-empty)
     */
    public synchronized boolean register(String userId, Collection<String> groups, String sourceId, Instant at) {
        touchSource(sourceId, at);
        if (isStale(sourceId, userId, at)) {
            LOGGER.debug(
                "register user={} source={} at={} ignored (a newer event was already applied)",
                userId,
                sourceId,
                at
            );
            return false;
        }
        recordEvent(sourceId, userId, at);
        Subscriber subscriber = registry.get(userId);
        boolean firstAppearance = subscriber == null;
        if (subscriber == null) {
            subscriber = new Subscriber(copyOf(groups));
            registry.put(userId, subscriber);
        } else if (groups != null) {
            subscriber.groups = copyOf(groups);
        }
        subscriber.sources.add(sourceId);
        LOGGER.debug(
            "register user={} source={} firstAppearance={} sources={}",
            userId,
            sourceId,
            firstAppearance,
            subscriber.sources
        );
        return firstAppearance;
    }

    /**
     * Removes {@code sourceId} from {@code userId}'s holders, unless a newer event already superseded it.
     *
     * @return {@code true} if that emptied the holder set and the user was dropped
     */
    public synchronized boolean unregister(String userId, String sourceId, Instant at) {
        if (isStale(sourceId, userId, at)) {
            LOGGER.debug(
                "unregister user={} source={} at={} ignored (a newer event was already applied)",
                userId,
                sourceId,
                at
            );
            return false;
        }
        recordEvent(sourceId, userId, at);
        Subscriber subscriber = registry.get(userId);
        if (subscriber == null) {
            LOGGER.debug("unregister user={} source={} (unknown user, ignored)", userId, sourceId);
            return false;
        }
        subscriber.sources.remove(sourceId);
        boolean dropped = subscriber.sources.isEmpty();
        if (dropped) {
            registry.remove(userId);
        }
        LOGGER.debug(
            "unregister user={} source={} dropped={} sources={}",
            userId,
            sourceId,
            dropped,
            subscriber.sources
        );
        return dropped;
    }

    /** Records liveness for an instance without changing any user's membership. */
    public synchronized void heartbeat(String sourceId, Instant at) {
        touchSource(sourceId, at);
        LOGGER.debug("heartbeat source={} at={}", sourceId, at);
    }

    /**
     * Removes instances not heard from within {@code threshold} of {@code now}, dropping them from
     * every user's holders and dropping any user left with no holders. This is the backstop for an
     * instance dying without sending UNREGISTERED for the users it held.
     *
     * @return the ids of users dropped as a result
     */
    public synchronized Set<String> expireInstances(Instant now, Duration threshold) {
        Instant deadline = now.minus(threshold);
        Set<String> deadSources = new HashSet<>();
        for (Map.Entry<String, Instant> source : lastSeenBySource.entrySet()) {
            if (source.getValue().isBefore(deadline)) {
                deadSources.add(source.getKey());
            }
        }
        Set<String> removedUsers = new LinkedHashSet<>();
        if (!deadSources.isEmpty()) {
            lastSeenBySource.keySet().removeAll(deadSources);
            lastEventBySourceUser.keySet().removeAll(deadSources);
            registry.entrySet().removeIf(user -> {
                user.getValue().sources.removeAll(deadSources);
                if (user.getValue().sources.isEmpty()) {
                    removedUsers.add(user.getKey());
                    return true;
                }
                return false;
            });
        }
        pruneEventHistory(deadline);
        LOGGER.debug("expireInstances deadSources={} droppedUsers={}", deadSources, removedUsers);
        return removedUsers;
    }

    /**
     * Reconciles this instance's holdings to its resync SNAPSHOT (adds listed users, drops the rest),
     * ordered per-source by {@code at} so it never undoes a newer live REGISTERED / UNREGISTERED.
     */
    public synchronized void applySnapshot(
        String sourceId,
        Collection<SubscriberRegistryMessage.Entry> entries,
        Instant at
    ) {
        touchSource(sourceId, at);
        if (entries == null) {
            LOGGER.debug("applySnapshot source={} entries=null (ignored)", sourceId);
            return;
        }
        LOGGER.debug("applySnapshot source={} entries={} at={}", sourceId, entries.size(), at);
        Set<String> usersInSnapshot = new HashSet<>();
        for (SubscriberRegistryMessage.Entry entry : entries) {
            usersInSnapshot.add(entry.userId());
            register(entry.userId(), entry.groups(), sourceId, at);
        }
        for (String heldUser : usersHeldBy(sourceId)) {
            if (!usersInSnapshot.contains(heldUser)) {
                unregister(heldUser, sourceId, at);
            }
        }
    }

    public synchronized boolean isWatching(String userId) {
        return registry.containsKey(userId);
    }

    public synchronized Set<String> groupsOf(String userId) {
        Subscriber subscriber = registry.get(userId);
        return subscriber == null ? Set.of() : Set.copyOf(subscriber.groups);
    }

    public synchronized Set<String> sourcesOf(String userId) {
        Subscriber subscriber = registry.get(userId);
        return subscriber == null ? Set.of() : Set.copyOf(subscriber.sources);
    }

    public synchronized Set<String> watchedUserIds() {
        return Set.copyOf(registry.keySet());
    }

    public synchronized int size() {
        return registry.size();
    }

    private void touchSource(String sourceId, Instant at) {
        lastSeenBySource.merge(sourceId, at, (current, candidate) -> candidate.isAfter(current) ? candidate : current);
    }

    /** Users currently held by {@code sourceId}, as a copy safe to iterate while mutating the registry. */
    private Set<String> usersHeldBy(String sourceId) {
        Set<String> held = new HashSet<>();
        for (Map.Entry<String, Subscriber> entry : registry.entrySet()) {
            if (entry.getValue().sources.contains(sourceId)) {
                held.add(entry.getKey());
            }
        }
        return held;
    }

    /** True when a newer event for the same instance and user has already been applied, making {@code at} stale. */
    private boolean isStale(String sourceId, String userId, Instant at) {
        Map<String, Instant> byUser = lastEventBySourceUser.get(sourceId);
        Instant last = byUser == null ? null : byUser.get(userId);
        return last != null && at.isBefore(last);
    }

    private void recordEvent(String sourceId, String userId, Instant at) {
        lastEventBySourceUser.computeIfAbsent(sourceId, key -> new HashMap<>()).put(userId, at);
    }

    /** Drops per-user event timestamps older than {@code deadline} to bound the retained history. */
    private void pruneEventHistory(Instant deadline) {
        lastEventBySourceUser.values().forEach(byUser -> byUser.values().removeIf(at -> at.isBefore(deadline)));
        lastEventBySourceUser.values().removeIf(Map::isEmpty);
    }

    private static Set<String> copyOf(Collection<String> groups) {
        return groups == null ? new HashSet<>() : new HashSet<>(groups);
    }
}
