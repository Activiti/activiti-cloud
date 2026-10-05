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

import java.util.Set;

/**
 * Immutable snapshot drained from a {@link PushedCountsRecomputeBuffer}. {@code taskIds},
 * {@code touchedGroupIds}, {@code namedUserIds} feed assigned/queued/running-processes;
 * {@code processInstanceIds}, {@code namedInitiatorIds} feed running-processes only.
 */
public record PushedCountsRecomputeWindow(
    Set<String> taskIds,
    Set<String> touchedGroupIds,
    Set<String> namedUserIds,
    Set<String> processInstanceIds,
    Set<String> namedInitiatorIds
) {
    public boolean isEmpty() {
        return taskIds.isEmpty() && processInstanceIds.isEmpty();
    }
}
