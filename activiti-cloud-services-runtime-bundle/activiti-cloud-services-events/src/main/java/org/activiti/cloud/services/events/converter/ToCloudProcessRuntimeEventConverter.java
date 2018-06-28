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
import org.activiti.runtime.api.event.CloudBPMNActivityCancelledEvent;
import org.activiti.runtime.api.event.CloudBPMNActivityCompletedEvent;
import org.activiti.runtime.api.event.CloudBPMNActivityStartedEvent;
import org.activiti.runtime.api.event.CloudProcessCancelledEvent;
import org.activiti.runtime.api.event.CloudProcessCompletedEvent;
import org.activiti.runtime.api.event.CloudProcessCreatedEvent;
import org.activiti.runtime.api.event.CloudProcessResumedEvent;
import org.activiti.runtime.api.event.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.CloudProcessSuspendedEvent;
import org.activiti.runtime.api.event.ProcessCancelled;
import org.activiti.runtime.api.event.ProcessCompleted;
import org.activiti.runtime.api.event.ProcessCreated;
import org.activiti.runtime.api.event.ProcessResumed;
import org.activiti.runtime.api.event.ProcessStarted;
import org.activiti.runtime.api.event.ProcessSuspended;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityStartedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessResumedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessSuspendedEventImpl;

public class ToCloudProcessRuntimeEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudProcessRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudProcessStartedEvent from(ProcessStarted event) {
        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl(event.getEntity(),
                                                                                                 event.getNestedProcessDefinitionId(),
                                                                                                 event.getNestedProcessInstanceId());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessStartedEvent);
        return cloudProcessStartedEvent;
    }

    public CloudProcessCreatedEvent from(ProcessCreated event) {
        CloudProcessCreatedEventImpl cloudEvent = new CloudProcessCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessResumedEvent from(ProcessResumed event) {
        CloudProcessResumedEventImpl cloudEvent = new CloudProcessResumedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessSuspendedEvent from(ProcessSuspended event) {
        CloudProcessSuspendedEventImpl cloudEvent = new CloudProcessSuspendedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessCompletedEvent from(ProcessCompleted event) {
        CloudProcessCompletedEventImpl cloudEvent = new CloudProcessCompletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudProcessCancelledEvent from(ProcessCancelled event) {
        CloudProcessCancelledEventImpl cloudEvent = new CloudProcessCancelledEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityStartedEvent from(BPMNActivityStarted event) {
        CloudBPMNActivityStartedEventImpl cloudEvent = new CloudBPMNActivityStartedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityCompletedEvent from(BPMNActivityCompleted event) {
        CloudBPMNActivityCompletedEventImpl cloudEvent = new CloudBPMNActivityCompletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudBPMNActivityCancelledEvent from(BPMNActivityCancelled event) {
        CloudBPMNActivityCancelledEventImpl cloudEvent = new CloudBPMNActivityCancelledEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

}
