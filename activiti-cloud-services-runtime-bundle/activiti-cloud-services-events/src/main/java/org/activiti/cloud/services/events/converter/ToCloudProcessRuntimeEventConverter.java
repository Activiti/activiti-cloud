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

import org.activiti.runtime.api.event.CloudProcessCancelledEvent;
import org.activiti.runtime.api.event.CloudProcessCompletedEvent;
import org.activiti.runtime.api.event.CloudProcessCreatedEvent;
import org.activiti.runtime.api.event.CloudProcessResumedEvent;
import org.activiti.runtime.api.event.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.CloudProcessSuspendedEvent;
import org.activiti.runtime.api.event.ProcessCancelledEvent;
import org.activiti.runtime.api.event.ProcessCompletedEvent;
import org.activiti.runtime.api.event.ProcessCreatedEvent;
import org.activiti.runtime.api.event.ProcessResumedEvent;
import org.activiti.runtime.api.event.ProcessStartedEvent;
import org.activiti.runtime.api.event.ProcessSuspendedEvent;
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

    public CloudProcessStartedEvent from(ProcessStartedEvent event) {
        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl(event.getEntity(),
                                                                                                 event.getNestedProcessDefinitionId(),
                                                                                                 event.getNestedProcessInstanceId());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessStartedEvent);
        return cloudProcessStartedEvent;
    }

    public CloudProcessCreatedEvent from(ProcessCreatedEvent event) {
        CloudProcessCreatedEventImpl cloudEvent = new CloudProcessCreatedEventImpl(event.getEntity());
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

}
