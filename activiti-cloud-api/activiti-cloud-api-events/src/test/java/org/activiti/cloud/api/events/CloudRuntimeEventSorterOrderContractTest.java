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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudApplicationDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageSentEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageWaitingEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerExecutedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFailedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerRetriesDecrementedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudMessageSubscriptionCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudStartMessageDeployedEventImpl;
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
import org.junit.jupiter.api.Test;

class CloudRuntimeEventSorterOrderContractTest {

    private static final List<List<Class<?>>> PRIORITY_LEVELS = List.of(
        List.of(
            CloudProcessCreatedEventImpl.class,
            CloudApplicationDeployedEventImpl.class,
            CloudBPMNErrorReceivedEventImpl.class,
            CloudBPMNMessageReceivedEventImpl.class,
            CloudBPMNMessageSentEventImpl.class,
            CloudBPMNMessageWaitingEventImpl.class,
            CloudBPMNTimerCancelledEventImpl.class,
            CloudBPMNTimerExecutedEventImpl.class,
            CloudBPMNTimerFailedEventImpl.class,
            CloudBPMNTimerFiredEventImpl.class,
            CloudBPMNTimerRetriesDecrementedEventImpl.class,
            CloudBPMNTimerScheduledEventImpl.class,
            CloudMessageSubscriptionCancelledEventImpl.class,
            CloudProcessDeployedEventImpl.class,
            CloudProcessResumedEventImpl.class,
            CloudStartMessageDeployedEventImpl.class
        ),
        List.of(
            CloudProcessStartedEventImpl.class,
            CloudProcessUpdatedEventImpl.class,
            CloudProcessSuspendedEventImpl.class
        ),
        List.of(CloudSequenceFlowTakenEventImpl.class),
        List.of(CloudBPMNActivityStartedEventImpl.class),
        List.of(CloudIntegrationRequestedEventImpl.class),
        List.of(CloudBPMNSignalReceivedEventImpl.class),
        List.of(CloudBPMNActivityCompletedEventImpl.class, CloudBPMNActivityCancelledEventImpl.class),
        List.of(CloudIntegrationResultReceivedEventImpl.class, CloudIntegrationErrorReceivedEventImpl.class),
        List.of(CloudTaskCreatedEventImpl.class),
        List.of(CloudTaskCandidateUserAddedEventImpl.class, CloudTaskCandidateGroupAddedEventImpl.class),
        List.of(CloudVariableCreatedEventImpl.class),
        List.of(CloudVariableUpdatedEventImpl.class),
        List.of(CloudVariableDeletedEventImpl.class),
        List.of(
            CloudTaskActivatedEventImpl.class,
            CloudTaskSuspendedEventImpl.class,
            CloudTaskAssignedEventImpl.class,
            CloudTaskUpdatedEventImpl.class
        ),
        List.of(CloudTaskCompletedEventImpl.class, CloudTaskCancelledEventImpl.class),
        List.of(CloudTaskCandidateUserRemovedEventImpl.class, CloudTaskCandidateGroupRemovedEventImpl.class),
        List.of(CloudProcessCompletedEventImpl.class, CloudProcessCancelledEventImpl.class),
        List.of(
            CloudProcessCandidateStarterUserAddedEventImpl.class,
            CloudProcessCandidateStarterGroupAddedEventImpl.class
        ),
        List.of(
            CloudProcessCandidateStarterUserRemovedEventImpl.class,
            CloudProcessCandidateStarterGroupRemovedEventImpl.class
        ),
        List.of(CloudProcessDeletedEventImpl.class)
    );

    private static final List<Class<?>> EXPECTED_ORDER = PRIORITY_LEVELS.stream().flatMap(List::stream).toList();

    @Test
    void should_sortEveryEventClassIntoTheExpectedOrder() {
        var unordered = oneEventPerClassWithPriorityLevelsReversed();

        var sorted = CloudRuntimeEventSorter.sort(unordered);

        assertThat(sorted)
            .extracting(Object::getClass)
            .as("sorted event classes: only update PRIORITY_LEVELS when the ordering contract really changes")
            .containsExactlyElementsOf(EXPECTED_ORDER);
    }

    private static List<CloudRuntimeEvent<?, ?>> oneEventPerClassWithPriorityLevelsReversed() {
        List<CloudRuntimeEvent<?, ?>> events = new ArrayList<>();
        for (int level = PRIORITY_LEVELS.size() - 1; level >= 0; level--) {
            PRIORITY_LEVELS.get(level).forEach(eventClass -> events.add(newInstanceOf(eventClass)));
        }
        return events;
    }

    private static CloudRuntimeEvent<?, ?> newInstanceOf(Class<?> eventClass) {
        try {
            return (CloudRuntimeEvent<?, ?>) eventClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate " + eventClass.getSimpleName(), e);
        }
    }
}
