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
import java.util.Objects;

/**
 * Count-channel message published by the query-consumer and relayed to the owning socket by
 * query-rest. The {@code scopeKey} (see {@link ScopeKeys}) says who the number is for and which
 * count type it belongs to; {@code count} is absolute (never a delta); {@code asOf} is when the count
 * was computed, not when the triggering event happened.
 */
public record CountChangedMessage(String scopeKey, long count, Instant asOf) {
    public CountChangedMessage {
        Objects.requireNonNull(scopeKey, "scopeKey");
        Objects.requireNonNull(asOf, "asOf");
    }
}
