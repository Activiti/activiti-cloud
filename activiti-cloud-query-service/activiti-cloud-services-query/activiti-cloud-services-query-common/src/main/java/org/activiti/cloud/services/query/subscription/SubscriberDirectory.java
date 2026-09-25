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

import java.util.Set;

/**
 * What the recompute pipeline needs to know about who is watching, without a compile-time
 * dependency on the query-rest module that actually holds the registry.
 */
public interface SubscriberDirectory {
    boolean isWatching(String userId);

    Set<String> groupsOf(String userId);

    Set<String> watchedUserIds();
}
