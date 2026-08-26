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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Remembers which group sets are currently worth counting for.
 * <p>
 * This closes the one gap an event cannot close on its own. A task event tells you the <em>task's</em>
 * candidate groups; a group-scoped count needs a <em>subscriber's</em> group set, and the two are not
 * interchangeable - the count for {@code {eng}} is not the count for {@code {eng, hr}}, and neither can
 * be derived from the other. So the emitter has to be told which group sets real users actually hold.
 * <p>
 * Rather than introduce a subscription protocol to collect them, group sets are recorded as a
 * by-product of ordinary read traffic: anyone whose count is worth pushing has necessarily just
 * fetched one. Entries expire, so the registry stays proportional to who is actually active.
 * <p>
 * <b>The TTL is a correctness parameter, not just a memory bound.</b> A subscriber that holds a
 * websocket open without re-fetching falls out of the registry when its entry expires and silently
 * stops receiving pushes. The TTL must therefore exceed the interval at which clients re-fetch their
 * counts.
 * <p>
 * Instances are thread-safe.
 */
public class SubscriberScopeRegistry {

    /** Normalized group set -> presence. Caffeine gives expiry and bounded size; the value is unused. */
    private final Cache<List<String>, Boolean> groupSets;

    public SubscriberScopeRegistry(Duration ttl, long maxSize) {
        this.groupSets = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
    }

    /**
     * Records that a subscriber holding this group set is active. Empty and null group collections are
     * ignored: they describe a user who can see nothing through groups, so there is no group-scoped
     * count to push them.
     */
    public void record(Collection<String> groups) {
        List<String> normalized = CountScopeKeys.normalizeGroups(groups);
        if (!normalized.isEmpty()) {
            groupSets.put(normalized, Boolean.TRUE);
        }
    }

    /**
     * The recorded group sets that contain at least one of {@code affectedGroups} - that is, exactly
     * the audiences whose count may have changed when those groups were touched.
     * <p>
     * A group set is returned when it intersects, not when it is contained: a user in
     * {@code {eng, hr}} sees tasks offered to {@code eng} alone, so an event touching {@code eng}
     * changes their count too.
     *
     * @return normalized group sets, each suitable for {@code TaskSpecification.forGroups(...)}
     */
    public Set<List<String>> groupSetsIntersecting(Collection<String> affectedGroups) {
        if (affectedGroups == null || affectedGroups.isEmpty()) {
            return Set.of();
        }
        Set<String> affected = Set.copyOf(CountScopeKeys.normalizeGroups(affectedGroups));
        return groupSets
            .asMap()
            .keySet()
            .stream()
            .filter(groupSet -> groupSet.stream().anyMatch(affected::contains))
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Every recorded group set. Needed for the case where a task carries no candidates at all: such a
     * task is visible through the "no candidate users or groups" branch to <em>every</em> audience, so
     * there is no set of affected groups to intersect against.
     *
     * @return normalized group sets, each suitable for {@code TaskSpecification.forGroups(...)}
     */
    public Set<List<String>> allGroupSets() {
        return Set.copyOf(groupSets.asMap().keySet());
    }

    /** Number of distinct group sets currently recorded. Intended for metrics and diagnostics. */
    public long size() {
        return groupSets.estimatedSize();
    }
}
