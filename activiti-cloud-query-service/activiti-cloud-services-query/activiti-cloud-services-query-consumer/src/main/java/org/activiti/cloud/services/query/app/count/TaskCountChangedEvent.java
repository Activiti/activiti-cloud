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

import java.util.List;

/**
 * The wire shape of a pushed count.
 *
 * @param scopeKey the audience this count is valid for
 * @param groups   the group set behind {@code scopeKey}, so a subscriber can match without parsing the key
 * @param count    number of tasks matching the pinned queued filter
 * @param asOf     epoch millis at which the count was read. <b>Consumers must use this to discard stale
 *                 messages:</b> concurrent event batches can publish out of order, so a client should
 *                 keep the highest {@code asOf} it has applied per {@code scopeKey} and ignore anything
 *                 older.
 */
public record TaskCountChangedEvent(String scopeKey, List<String> groups, long count, long asOf) {
    public static TaskCountChangedEvent of(TaskCountChange change, long asOf) {
        return new TaskCountChangedEvent(change.scopeKey(), change.groups(), change.count(), asOf);
    }
}
