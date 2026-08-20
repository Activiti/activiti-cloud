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
package org.activiti.cloud.api.events;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskActivatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskSuspendedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;

public final class CloudRuntimeEventSorter {

    private static final Map<Class<? extends CloudRuntimeEvent>, Integer> ORDER = Map.ofEntries(
        Map.entry(CloudRuntimeEvent.class, 0),
        Map.entry(CloudProcessCreatedEventImpl.class, 0),
        Map.entry(CloudProcessStartedEventImpl.class, 1),
        Map.entry(CloudProcessUpdatedEventImpl.class, 1),
        Map.entry(CloudProcessSuspendedEventImpl.class, 1),
        Map.entry(CloudSequenceFlowTakenEventImpl.class, 2),
        Map.entry(CloudBPMNActivityStartedEventImpl.class, 3),
        Map.entry(CloudIntegrationRequestedEventImpl.class, 4),
        Map.entry(CloudBPMNSignalReceivedEventImpl.class, 5),
        Map.entry(CloudBPMNActivityCompletedEventImpl.class, 6),
        Map.entry(CloudBPMNActivityCancelledEventImpl.class, 6),
        Map.entry(CloudIntegrationResultReceivedEventImpl.class, 7),
        Map.entry(CloudIntegrationErrorReceivedEventImpl.class, 7),
        Map.entry(CloudTaskCreatedEventImpl.class, 8),
        Map.entry(CloudTaskCandidateUserAddedEventImpl.class, 9),
        Map.entry(CloudTaskCandidateGroupAddedEventImpl.class, 9),
        Map.entry(CloudVariableCreatedEventImpl.class, 10),
        Map.entry(CloudVariableUpdatedEventImpl.class, 11),
        Map.entry(CloudVariableDeletedEventImpl.class, 12),
        Map.entry(CloudTaskActivatedEventImpl.class, 13),
        Map.entry(CloudTaskSuspendedEventImpl.class, 13),
        Map.entry(CloudTaskAssignedEventImpl.class, 13),
        Map.entry(CloudTaskUpdatedEventImpl.class, 13),
        Map.entry(CloudTaskCompletedEventImpl.class, 14),
        Map.entry(CloudTaskCancelledEventImpl.class, 14),
        Map.entry(CloudTaskCandidateUserRemovedEventImpl.class, 15),
        Map.entry(CloudTaskCandidateGroupRemovedEventImpl.class, 15),
        Map.entry(CloudProcessCompletedEventImpl.class, 16),
        Map.entry(CloudProcessCancelledEventImpl.class, 16),
        Map.entry(CloudProcessCandidateStarterUserAddedEventImpl.class, 17),
        Map.entry(CloudProcessCandidateStarterGroupAddedEventImpl.class, 17),
        Map.entry(CloudProcessCandidateStarterUserRemovedEventImpl.class, 18),
        Map.entry(CloudProcessCandidateStarterGroupRemovedEventImpl.class, 18),
        Map.entry(CloudProcessDeletedEventImpl.class, 19)
    );

    private static final Comparator<CloudRuntimeEvent<?, ?>> BY_EVENT_CLASS = Comparator.comparing(event ->
        Optional.ofNullable(ORDER.get(event.getClass())).orElseGet(() -> ORDER.get(CloudRuntimeEvent.class))
    );

    private static final Comparator<CloudRuntimeEvent<?, ?>> BY_TIMESTAMP = Comparator.comparingLong(
        CloudRuntimeEvent::getTimestamp
    );

    private static final Comparator<CloudRuntimeEvent<?, ?>> COMPARATOR = BY_EVENT_CLASS.thenComparing(BY_TIMESTAMP);

    private CloudRuntimeEventSorter() {}

    public static <T extends CloudRuntimeEvent<?, ?>> List<T> sort(List<T> events) {
        return events.stream().sorted(COMPARATOR).toList();
    }
}
