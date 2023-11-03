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
package org.activiti.cloud.services.events.converter;

import org.activiti.api.process.model.events.BPMNActivityCancelledEvent;
import org.activiti.api.process.model.events.BPMNActivityCompletedEvent;
import org.activiti.api.process.model.events.BPMNActivityStartedEvent;
import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageSentEvent;
import org.activiti.api.process.model.events.BPMNMessageWaitingEvent;
import org.activiti.api.process.model.events.BPMNSequenceFlowTakenEvent;
import org.activiti.api.process.model.events.BPMNSignalReceivedEvent;
import org.activiti.api.process.model.events.BPMNTimerCancelledEvent;
import org.activiti.api.process.model.events.BPMNTimerExecutedEvent;
import org.activiti.api.process.model.events.BPMNTimerFailedEvent;
import org.activiti.api.process.model.events.BPMNTimerFiredEvent;
import org.activiti.api.process.model.events.BPMNTimerRetriesDecrementedEvent;
import org.activiti.api.process.model.events.BPMNTimerScheduledEvent;
import org.activiti.api.process.model.events.MessageSubscriptionCancelledEvent;
import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.api.process.model.events.StartMessageDeployedEvent;
import org.activiti.api.process.runtime.events.ProcessCancelledEvent;
import org.activiti.api.process.runtime.events.ProcessCompletedEvent;
import org.activiti.api.process.runtime.events.ProcessCreatedEvent;
import org.activiti.api.process.runtime.events.ProcessResumedEvent;
import org.activiti.api.process.runtime.events.ProcessStartedEvent;
import org.activiti.api.process.runtime.events.ProcessSuspendedEvent;
import org.activiti.api.process.runtime.events.ProcessUpdatedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageSentEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageWaitingEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFailedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerRetriesDecrementedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessResumedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessSuspendedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessUpdatedEvent;
import org.activiti.cloud.api.process.model.events.CloudSequenceFlowTakenEvent;
import org.activiti.cloud.api.process.model.events.CloudStartMessageDeployedEvent;
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
import org.activiti.cloud.api.process.model.impl.events.CloudMessageSubscriptionCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudStartMessageDeployedEventImpl;

public class ToCloudProcessRuntimeEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    private final ProcessAuditServiceInfoAppender processAuditServiceInfoAppender;

    public ToCloudProcessRuntimeEventConverter(
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        ProcessAuditServiceInfoAppender processAuditServiceInfoAppender
    ) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.processAuditServiceInfoAppender = processAuditServiceInfoAppender;
    }

    public CloudProcessStartedEvent from(ProcessStartedEvent event) {
        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl(
            event.getEntity(),
            event.getNestedProcessDefinitionId(),
            event.getNestedProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessStartedEvent);
        this.processAuditServiceInfoAppender.appendAuditServiceInfoTo(cloudProcessStartedEvent);
        return cloudProcessStartedEvent;
    }

    public CloudProcessCreatedEvent from(ProcessCreatedEvent event) {
        CloudProcessCreatedEventImpl cloudEvent = new CloudProcessCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessUpdatedEvent from(ProcessUpdatedEvent event) {
        CloudProcessUpdatedEventImpl cloudEvent = new CloudProcessUpdatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessResumedEvent from(ProcessResumedEvent event) {
        CloudProcessResumedEventImpl cloudEvent = new CloudProcessResumedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessSuspendedEvent from(ProcessSuspendedEvent event) {
        CloudProcessSuspendedEventImpl cloudEvent = new CloudProcessSuspendedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessCompletedEvent from(ProcessCompletedEvent event) {
        CloudProcessCompletedEventImpl cloudEvent = new CloudProcessCompletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessCancelledEvent from(ProcessCancelledEvent event) {
        CloudProcessCancelledEventImpl cloudEvent = new CloudProcessCancelledEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityStartedEvent from(BPMNActivityStartedEvent event) {
        CloudBPMNActivityStartedEventImpl cloudEvent = new CloudBPMNActivityStartedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityCompletedEvent from(BPMNActivityCompletedEvent event) {
        CloudBPMNActivityCompletedEventImpl cloudEvent = new CloudBPMNActivityCompletedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityCancelledEvent from(BPMNActivityCancelledEvent event) {
        CloudBPMNActivityCancelledEventImpl cloudEvent = new CloudBPMNActivityCancelledEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId(),
            ""
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNSignalReceivedEvent from(BPMNSignalReceivedEvent event) {
        CloudBPMNSignalReceivedEventImpl cloudEvent = new CloudBPMNSignalReceivedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudSequenceFlowTakenEvent from(BPMNSequenceFlowTakenEvent event) {
        CloudSequenceFlowTakenEventImpl cloudEvent = new CloudSequenceFlowTakenEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessDeployedEvent from(ProcessDeployedEvent event) {
        CloudProcessDeployedEventImpl cloudEvent = new CloudProcessDeployedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudStartMessageDeployedEvent from(StartMessageDeployedEvent event) {
        CloudStartMessageDeployedEventImpl cloudEvent = CloudStartMessageDeployedEventImpl
            .builder()
            .withEntity(event.getEntity())
            .build();
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudMessageSubscriptionCancelledEvent from(MessageSubscriptionCancelledEvent event) {
        CloudMessageSubscriptionCancelledEventImpl cloudEvent = CloudMessageSubscriptionCancelledEventImpl
            .builder()
            .withEntity(event.getEntity())
            .build();
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNTimerFiredEvent from(BPMNTimerFiredEvent event) {
        CloudBPMNTimerFiredEventImpl cloudEvent = new CloudBPMNTimerFiredEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNTimerScheduledEvent from(BPMNTimerScheduledEvent event) {
        CloudBPMNTimerScheduledEventImpl cloudEvent = new CloudBPMNTimerScheduledEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNTimerCancelledEvent from(BPMNTimerCancelledEvent event) {
        CloudBPMNTimerCancelledEventImpl cloudEvent = new CloudBPMNTimerCancelledEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNTimerFailedEvent from(BPMNTimerFailedEvent event) {
        CloudBPMNTimerFailedEventImpl cloudEvent = new CloudBPMNTimerFailedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNTimerExecutedEvent from(BPMNTimerExecutedEvent event) {
        CloudBPMNTimerExecutedEventImpl cloudEvent = new CloudBPMNTimerExecutedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNTimerRetriesDecrementedEvent from(BPMNTimerRetriesDecrementedEvent event) {
        CloudBPMNTimerRetriesDecrementedEventImpl cloudEvent = new CloudBPMNTimerRetriesDecrementedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNMessageSentEvent from(BPMNMessageSentEvent event) {
        CloudBPMNMessageSentEventImpl cloudEvent = new CloudBPMNMessageSentEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNMessageReceivedEvent from(BPMNMessageReceivedEvent event) {
        CloudBPMNMessageReceivedEventImpl cloudEvent = new CloudBPMNMessageReceivedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNMessageWaitingEvent from(BPMNMessageWaitingEvent event) {
        CloudBPMNMessageWaitingEventImpl cloudEvent = new CloudBPMNMessageWaitingEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNErrorReceivedEvent from(BPMNErrorReceivedEvent event) {
        CloudBPMNErrorReceivedEventImpl cloudEvent = new CloudBPMNErrorReceivedEventImpl(
            event.getEntity(),
            event.getEntity().getProcessDefinitionId(),
            event.getEntity().getProcessInstanceId()
        );
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }
}
