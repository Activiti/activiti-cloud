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

/**
 * A recomputed count for one audience.
 * <p>
 * {@code count} is a snapshot, not a transactional guarantee: two batches committing concurrently can
 * produce changes that are published out of order. Consumers should keep the highest {@code asOf} they
 * have applied per {@code scopeKey} and discard anything older - see the {@code asOf} stamped on the
 * message at publish time rather than here, so that this record stays free of clock access.
 *
 * @param scopeKey the audience the count is valid for, as built by {@link CountScopeKeys}
 * @param groups   the normalized group set behind the count. For a {@code queued:} scope this is the
 *                 subscriber's own membership - not part of their identity, but the bucket the count was
 *                 computed in, which is what makes a wrong number diagnosable from the message alone
 * @param count    number of tasks matching {@link PushedTaskCountFilter#QUEUED} for that audience
 */
public record TaskCountChange(String scopeKey, List<String> groups, long count) {
    /**
     * One user's queued-task badge. This is the shape that gets published: a group-scoped count is not a
     * badge, because it omits every branch that depends on who is asking.
     */
    public static TaskCountChange forQueued(String userId, Collection<String> groups, long count) {
        return new TaskCountChange(CountScopeKeys.forQueued(userId), CountScopeKeys.normalizeGroups(groups), count);
    }

    /**
     * A count valid for everyone holding exactly {@code groups}. Retained because it is the shared half of a
     * per-user count, not because it is published on its own.
     */
    public static TaskCountChange forGroups(List<String> groups, long count) {
        return new TaskCountChange(CountScopeKeys.forGroups(groups), List.copyOf(groups), count);
    }
}
