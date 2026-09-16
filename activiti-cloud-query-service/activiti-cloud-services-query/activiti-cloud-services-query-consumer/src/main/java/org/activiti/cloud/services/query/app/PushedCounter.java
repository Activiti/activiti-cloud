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

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys;

/**
 * Recomputes one pushed-count type for the users touched by an event window. Returns one absolute
 * {@link CountChangedMessage} per affected user, including zero for a user who now has none. There is
 * one implementation per {@link ScopeKeys.PushedCountType}; the recompute pipeline invokes them.
 */
public interface PushedCounter {
    ScopeKeys.PushedCountType type();

    List<CountChangedMessage> countFor(Collection<String> affectedUserIds, Instant asOf);
}
