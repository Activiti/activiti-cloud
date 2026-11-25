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
package org.activiti.cloud.services.events.assignment;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public final class TaskAssignmentHistoryContext {

    private static final ThreadLocal<Change> CURRENT_CHANGE = new ThreadLocal<>();

    private TaskAssignmentHistoryContext() {}

    public static <T> T withChange(String action, String reason, Collection<String> taskIds, Supplier<T> operation) {
        Change previousChange = CURRENT_CHANGE.get();
        CURRENT_CHANGE.set(new Change(action, reason, Set.copyOf(taskIds)));
        try {
            return operation.get();
        } finally {
            if (previousChange == null) {
                CURRENT_CHANGE.remove();
            } else {
                CURRENT_CHANGE.set(previousChange);
            }
        }
    }

    public static Change currentChange() {
        return CURRENT_CHANGE.get();
    }

    public record Change(String action, String reason, Set<String> taskIds) {}
}
