/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.converter;

import org.activiti.runtime.api.event.BPMNActivityCancelled;
import org.activiti.runtime.api.event.BPMNActivityCompleted;
import org.activiti.runtime.api.event.BPMNActivityStarted;
import org.activiti.runtime.api.event.CloudBPMNActivityCancelled;
import org.activiti.runtime.api.event.CloudBPMNActivityCompleted;
import org.activiti.runtime.api.event.CloudBPMNActivityStarted;
import org.activiti.runtime.api.event.CloudProcessCancelled;
import org.activiti.runtime.api.event.CloudProcessCompleted;
import org.activiti.runtime.api.event.CloudProcessCreated;
import org.activiti.runtime.api.event.CloudProcessResumed;
import org.activiti.runtime.api.event.CloudProcessStarted;
import org.activiti.runtime.api.event.CloudProcessSuspended;
import org.activiti.runtime.api.event.CloudSequenceFlowTaken;
import org.activiti.runtime.api.event.ProcessCancelled;
import org.activiti.runtime.api.event.ProcessCompleted;
import org.activiti.runtime.api.event.ProcessCreated;
import org.activiti.runtime.api.event.ProcessResumed;
import org.activiti.runtime.api.event.ProcessStarted;
import org.activiti.runtime.api.event.ProcessSuspended;
import org.activiti.runtime.api.event.SequenceFlowTaken;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityStartedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessResumedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessSuspendedEventImpl;
import org.activiti.runtime.api.event.impl.CloudSequenceFlowTakenImpl;

public class ToCloudProcessRuntimeEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudProcessRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudProcessStarted from(ProcessStarted event) {
        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl(event.getEntity(),
                                                                                                 event.getNestedProcessDefinitionId(),
                                                                                                 event.getNestedProcessInstanceId());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessStartedEvent);
        return cloudProcessStartedEvent;
    }

    public CloudProcessCreated from(ProcessCreated event) {
        CloudProcessCreatedEventImpl cloudEvent = new CloudProcessCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessResumed from(ProcessResumed event) {
        CloudProcessResumedEventImpl cloudEvent = new CloudProcessResumedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessSuspended from(ProcessSuspended event) {
        CloudProcessSuspendedEventImpl cloudEvent = new CloudProcessSuspendedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessCompleted from(ProcessCompleted event) {
        CloudProcessCompletedEventImpl cloudEvent = new CloudProcessCompletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessCancelled from(ProcessCancelled event) {
        CloudProcessCancelledEventImpl cloudEvent = new CloudProcessCancelledEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityStarted from(BPMNActivityStarted event) {
        CloudBPMNActivityStartedEventImpl cloudEvent = new CloudBPMNActivityStartedEventImpl(event.getEntity(),
                                                                                             event.getEntity().getProcessDefinitionId(),
                                                                                             event.getEntity().getProcessInstanceId());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityCompleted from(BPMNActivityCompleted event) {
        CloudBPMNActivityCompletedEventImpl cloudEvent = new CloudBPMNActivityCompletedEventImpl(event.getEntity(),
                                                                                                 event.getEntity().getProcessDefinitionId(),
                                                                                                 event.getEntity().getProcessInstanceId());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityCancelled from(BPMNActivityCancelled event) {
        CloudBPMNActivityCancelledEventImpl cloudEvent = new CloudBPMNActivityCancelledEventImpl(event.getEntity(),
                                                                                                 event.getEntity().getProcessDefinitionId(),
                                                                                                 event.getEntity().getProcessInstanceId(),
                                                                                                 "");
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudSequenceFlowTaken from(SequenceFlowTaken event) {
        CloudSequenceFlowTakenImpl cloudEvent = new CloudSequenceFlowTakenImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

}
