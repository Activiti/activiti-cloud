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
package org.activiti.cloud.services.query.app.count;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Who is currently watching a task count, and which groups they hold.
 * <p>
 * This is the only place a user's group membership exists in the query service. <b>There is no user table
 * and no user-to-group table</b> in the query model, and no join that could reach one: membership arrives
 * purely as an input, read off the caller's token by the REST tier. So a user who happens to have no
 * {@code candidate_user} row anywhere cannot be produced by any query - they exist here or nowhere. That is
 * why this registry cannot be replaced by "just query the group's members", and why it is a prerequisite for
 * publishing per-user counts at all.
 * <p>
 * It is also what closes the gap an event cannot close. An event tells you the <em>task's</em> candidate
 * groups; deciding whose count moved needs the reverse mapping, and only the registry has it.
 * <p>
 * <b>The expiry is still a correctness parameter, not yet a leak guard.</b> Entries are written as a
 * by-product of ordinary read traffic - anyone whose count is worth pushing has necessarily just fetched one
 * - so a subscriber that holds a websocket open without ever re-fetching falls out and silently stops
 * receiving pushes. The intended fix is to write on websocket connect and {@link #deregister(String)} on
 * disconnect, at which point expiry degrades to what it should be: a guard against entries whose disconnect
 * was lost. Until the notifications service does that, the expiry must exceed the interval at which clients
 * re-fetch.
 * <p>
 * Instances are thread-safe.
 */
public class SubscriberScopeRegistry {

    /** User id -> their normalized group set. Caffeine gives expiry and a bounded size. */
    private final Cache<String, List<String>> subscribers;

    public SubscriberScopeRegistry(Duration ttl, long maxSize) {
        this.subscribers = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
    }

    /**
     * Records that {@code userId} is watching, holding {@code groups}.
     * <p>
     * An empty group collection is recorded, not ignored: a user in no groups still has a queued count - the
     * tasks they are individually named on, plus the tasks that have no candidates at all. It was ignorable
     * only while counts were group-scoped.
     */
    public void record(String userId, Collection<String> groups) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        subscribers.put(userId, CountScopeKeys.normalizeGroups(groups));
    }

    /** Drops a subscriber, for when their websocket closes. */
    public void deregister(String userId) {
        if (userId != null) {
            subscribers.invalidate(userId);
        }
    }

    /**
     * The subscribers holding at least one of {@code affectedGroups} - that is, the people whose queued count
     * may have moved when those groups were touched.
     * <p>
     * Intersection, not containment: a user in {@code {eng, hr}} sees tasks offered to {@code eng} alone, so
     * an event touching {@code eng} changes their count too.
     * <p>
     * An in-memory scan, bounded by the registry's maximum size. No query is involved, because none could be:
     * see the class javadoc.
     */
    public Set<String> subscribersHoldingAnyOf(Collection<String> affectedGroups) {
        if (affectedGroups == null || affectedGroups.isEmpty()) {
            return Set.of();
        }
        Set<String> affected = Set.copyOf(CountScopeKeys.normalizeGroups(affectedGroups));
        return subscribers
            .asMap()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().stream().anyMatch(affected::contains))
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Every subscriber. Needed for the case where a task carries no candidates at all: such a task is visible
     * through the specification's "no candidate users or groups" branch to <em>everyone</em>, so there is no
     * set of affected groups to narrow by.
     */
    public Set<String> allSubscribers() {
        return Set.copyOf(subscribers.asMap().keySet());
    }

    /**
     * Whether this user is watching. Used to filter the candidate users named on the changed tasks down to
     * the ones worth counting for - the door through which a user reaches their count when none of their
     * groups was touched at all.
     */
    public boolean isRegistered(String userId) {
        return userId != null && subscribers.getIfPresent(userId) != null;
    }

    /**
     * The group set recorded for this user, or empty if they are unknown or hold no groups. Two subscribers
     * with equal group sets share a count query, which is what keeps the cost independent of headcount.
     */
    public List<String> groupsOf(String userId) {
        if (userId == null) {
            return List.of();
        }
        List<String> groups = subscribers.getIfPresent(userId);
        return groups == null ? List.of() : groups;
    }

    /** Number of subscribers currently recorded. Intended for metrics and diagnostics. */
    public long size() {
        return subscribers.estimatedSize();
    }
}
