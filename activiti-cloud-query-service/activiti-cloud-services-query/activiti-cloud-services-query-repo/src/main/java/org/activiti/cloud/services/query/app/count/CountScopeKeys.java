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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Builds and parses the keys that identify a count scope - the audience a single count is valid for.
 * <p>
 * The two shapes that are actually pushed name a single user, because a count that names anything less
 * specific is not the number a badge shows. They are disjoint by definition - {@code assignee = me}
 * versus {@code assignee IS NULL} - so a client never has to add or reconcile them:
 * <ul>
 *   <li>{@code queued:<userId>} - unassigned tasks the user could pick up: they are a candidate user,
 *       or one of their groups is a candidate group, or the task has no candidates at all.</li>
 *   <li>{@code assigned:<userId>} - tasks assigned to the user.</li>
 * </ul>
 * Three older shapes remain, none of them per-user badges:
 * <ul>
 *   <li>{@code user:<userId>} - a task count that depends on the user's identity, so it is valid for
 *       exactly one user.</li>
 *   <li>{@code puser:<userId>} - the process-instance equivalent. Processes have no group branch, so
 *       there is no group-scoped variant.</li>
 *   <li>{@code groups:<g1>,<g2>,...} - a task count that depends only on group membership, so it is
 *       valid for every user holding exactly that group set. Cheap and shareable, but <b>not a badge</b>:
 *       it deliberately omits the assignee, owner and candidate-user branches, so it is smaller than
 *       what any of those users actually sees. Retained because it is how a per-user count is
 *       <em>computed</em> - see {@link TaskCountRecomputer} - not how one is published.</li>
 * </ul>
 * Group sets are normalized (null-free, de-duplicated, sorted) so that the same membership always
 * produces the same key regardless of the order the identity provider returned it in.
 */
public final class CountScopeKeys {

    public static final String QUEUED_PREFIX = "queued:";
    public static final String ASSIGNED_PREFIX = "assigned:";
    public static final String USER_PREFIX = "user:";
    public static final String PROCESS_USER_PREFIX = "puser:";
    public static final String GROUPS_PREFIX = "groups:";

    private static final String GROUP_SEPARATOR = ",";

    private CountScopeKeys() {}

    /**
     * Null-free, de-duplicated, sorted view of a group collection. Matches the normalization
     * {@code TaskControllerHelper} already applies when building its count cache key, so a scope key
     * and a cache key agree on what "the same group set" means.
     */
    public static List<String> normalizeGroups(Collection<String> groups) {
        if (groups == null) {
            return List.of();
        }
        return groups.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    /**
     * @throws IllegalArgumentException if the group set is empty, or if a group id contains the
     *                                  separator - either would make the key ambiguous, and an empty
     *                                  group set would name an unrestricted count.
     */
    public static String forGroups(Collection<String> groups) {
        List<String> normalized = normalizeGroups(groups);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("a group scope key needs at least one group");
        }
        normalized
            .stream()
            .filter(group -> group.contains(GROUP_SEPARATOR))
            .findFirst()
            .ifPresent(group -> {
                throw new IllegalArgumentException(
                    "group id must not contain '" +
                        GROUP_SEPARATOR +
                        "', it would make the scope key ambiguous: " +
                        group
                );
            });
        return GROUPS_PREFIX + String.join(GROUP_SEPARATOR, normalized);
    }

    /**
     * The key for a user's queued-task badge: unassigned tasks they could pick up. This is the shape the
     * event consumer publishes, and it is the count {@code POST /v1/tasks/count} returns for that user, so
     * the pushed and polled numbers agree by construction.
     */
    public static String forQueued(String userId) {
        return QUEUED_PREFIX + requireUserId(userId);
    }

    /**
     * The key for a user's assigned-task badge. Disjoint from {@link #forQueued(String)} by definition:
     * {@code assignee = me} cannot also be {@code assignee IS NULL}.
     */
    public static String forAssigned(String userId) {
        return ASSIGNED_PREFIX + requireUserId(userId);
    }

    public static String forUser(String userId) {
        return USER_PREFIX + userId;
    }

    public static String forProcessUser(String userId) {
        return PROCESS_USER_PREFIX + userId;
    }

    public static boolean isGroupScope(String scopeKey) {
        return scopeKey != null && scopeKey.startsWith(GROUPS_PREFIX);
    }

    /**
     * Inverse of {@link #forGroups(Collection)}.
     *
     * @throws IllegalArgumentException if the key is not a group scope key
     */
    public static List<String> groupsOf(String scopeKey) {
        if (!isGroupScope(scopeKey)) {
            throw new IllegalArgumentException("not a group scope key: " + scopeKey);
        }
        return List.of(scopeKey.substring(GROUPS_PREFIX.length()).split(GROUP_SEPARATOR));
    }

    /**
     * A per-user key with no user in it would be indistinguishable from the prefix itself, and a subscriber
     * matching on their own subject claim would never match it - so it is rejected rather than published.
     */
    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("a per-user scope key needs a user id");
        }
        return userId;
    }
}
