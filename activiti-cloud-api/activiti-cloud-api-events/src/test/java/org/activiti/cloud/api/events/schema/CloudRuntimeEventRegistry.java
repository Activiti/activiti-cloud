/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.api.events.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.activiti.api.model.shared.event.RuntimeEvent;
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
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterUserRemovedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupAddedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCandidateStarterGroupRemovedEventImpl;
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


public class CloudRuntimeEventRegistry {

    public Map<String, Class<?>> buildRegistry() {
        List<RuntimeEvent<?, ?>> eventImplementations = new ArrayList<>();
        eventImplementations.add(new CloudBPMNActivityStartedEventImpl());
        eventImplementations.add(new CloudBPMNActivityCancelledEventImpl());
        eventImplementations.add(new CloudBPMNActivityCompletedEventImpl());
        eventImplementations.add(new CloudBPMNErrorReceivedEventImpl());
        eventImplementations.add(new CloudBPMNSignalReceivedEventImpl());
        eventImplementations.add(new CloudBPMNTimerFiredEventImpl());
        eventImplementations.add(new CloudBPMNTimerCancelledEventImpl());
        eventImplementations.add(new CloudBPMNTimerScheduledEventImpl());
        eventImplementations.add(new CloudBPMNTimerExecutedEventImpl());
        eventImplementations.add(new CloudBPMNTimerFailedEventImpl());
        eventImplementations.add(new CloudBPMNTimerRetriesDecrementedEventImpl());
        eventImplementations.add(new CloudBPMNMessageWaitingEventImpl());
        eventImplementations.add(new CloudBPMNMessageReceivedEventImpl());
        eventImplementations.add(new CloudBPMNMessageSentEventImpl());
        eventImplementations.add(new CloudIntegrationRequestedEventImpl());
        eventImplementations.add(new CloudIntegrationResultReceivedEventImpl());
        eventImplementations.add(new CloudIntegrationErrorReceivedEventImpl());
        eventImplementations.add(new CloudProcessDeployedEventImpl());
        eventImplementations.add(new CloudProcessCreatedEventImpl());
        eventImplementations.add(new CloudProcessStartedEventImpl());
        eventImplementations.add(new CloudProcessCompletedEventImpl());
        eventImplementations.add(new CloudProcessCancelledEventImpl());
        eventImplementations.add(new CloudProcessSuspendedEventImpl());
        eventImplementations.add(new CloudProcessResumedEventImpl());
        eventImplementations.add(new CloudProcessUpdatedEventImpl());
        eventImplementations.add(new CloudProcessDeletedEventImpl());
        eventImplementations.add(new CloudSequenceFlowTakenEventImpl());
        eventImplementations.add(new CloudStartMessageDeployedEventImpl());
        eventImplementations.add(new CloudMessageSubscriptionCancelledEventImpl());
        eventImplementations.add(new CloudTaskCreatedEventImpl());
        eventImplementations.add(new CloudTaskUpdatedEventImpl());
        eventImplementations.add(new CloudTaskAssignedEventImpl());
        eventImplementations.add(new CloudTaskCompletedEventImpl());
        eventImplementations.add(new CloudTaskSuspendedEventImpl());
        eventImplementations.add(new CloudTaskActivatedEventImpl());
        eventImplementations.add(new CloudTaskCancelledEventImpl());
        eventImplementations.add(new CloudTaskCandidateUserAddedEventImpl());
        eventImplementations.add(new CloudTaskCandidateUserRemovedEventImpl());
        eventImplementations.add(new CloudTaskCandidateGroupAddedEventImpl());
        eventImplementations.add(new CloudTaskCandidateGroupRemovedEventImpl());
        eventImplementations.add(new CloudVariableCreatedEventImpl());
        eventImplementations.add(new CloudVariableUpdatedEventImpl());
        eventImplementations.add(new CloudVariableDeletedEventImpl());
        eventImplementations.add(new CloudApplicationDeployedEventImpl());
        eventImplementations.add(new CloudProcessCandidateStarterUserAddedEventImpl());
        eventImplementations.add(new CloudProcessCandidateStarterUserRemovedEventImpl());
        eventImplementations.add(new CloudProcessCandidateStarterGroupAddedEventImpl());
        eventImplementations.add(new CloudProcessCandidateStarterGroupRemovedEventImpl());
        return eventImplementations
                .stream()
                .collect(Collectors.toMap(
                        event -> event.getEventType().name(),
                        this::findInterface));
    }

    private Class<?> findInterface(RuntimeEvent<?, ?> eventImplementationClass) {
        return Arrays.stream(eventImplementationClass.getClass().getInterfaces())
                .filter(eventInterFace ->
                                eventImplementationClass.getClass().getSimpleName().contains(eventInterFace.getSimpleName()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Unable to find interface for " + eventImplementationClass.getClass()));
    }
}
