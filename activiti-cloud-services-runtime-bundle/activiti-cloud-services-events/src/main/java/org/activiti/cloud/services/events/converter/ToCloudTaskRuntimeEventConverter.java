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

import org.activiti.runtime.api.event.CloudTaskActivatedEvent;
import org.activiti.runtime.api.event.CloudTaskAssignedEvent;
import org.activiti.runtime.api.event.CloudTaskCancelledEvent;
import org.activiti.runtime.api.event.CloudTaskCandidateGroupAddedEvent;
import org.activiti.runtime.api.event.CloudTaskCandidateUserAddedEvent;
import org.activiti.runtime.api.event.CloudTaskCompletedEvent;
import org.activiti.runtime.api.event.CloudTaskCreatedEvent;
import org.activiti.runtime.api.event.CloudTaskSuspendedEvent;
import org.activiti.runtime.api.event.TaskActivated;
import org.activiti.runtime.api.event.TaskAssigned;
import org.activiti.runtime.api.event.TaskCancelled;
import org.activiti.runtime.api.event.TaskCandidateGroupAdded;
import org.activiti.runtime.api.event.TaskCandidateUserAdded;
import org.activiti.runtime.api.event.TaskCompleted;
import org.activiti.runtime.api.event.TaskCreated;
import org.activiti.runtime.api.event.TaskSuspended;
import org.activiti.runtime.api.event.impl.CloudTaskActivatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskAssignedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCompletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskSuspendedEventImpl;

public class ToCloudTaskRuntimeEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudTaskRuntimeEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudTaskCreatedEvent from(TaskCreated event) {
        CloudTaskCreatedEventImpl cloudEvent = new CloudTaskCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskAssignedEvent from(TaskAssigned event) {
        CloudTaskAssignedEventImpl cloudEvent = new CloudTaskAssignedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskCompletedEvent from(TaskCompleted event) {
        CloudTaskCompletedEventImpl cloudEvent = new CloudTaskCompletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskCancelledEvent from(TaskCancelled event) {
        CloudTaskCancelledEventImpl cloudEvent = new CloudTaskCancelledEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskSuspendedEvent from(TaskSuspended event) {
        CloudTaskSuspendedEventImpl cloudEvent = new CloudTaskSuspendedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskActivatedEvent from(TaskActivated event) {
        CloudTaskActivatedEventImpl cloudEvent = new CloudTaskActivatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskCandidateUserAddedEvent from(TaskCandidateUserAdded event){
        CloudTaskCandidateUserAddedEventImpl cloudEvent = new CloudTaskCandidateUserAddedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudTaskCandidateGroupAddedEvent from(TaskCandidateGroupAdded event){
        CloudTaskCandidateGroupAddedEventImpl cloudEvent = new CloudTaskCandidateGroupAddedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

}
