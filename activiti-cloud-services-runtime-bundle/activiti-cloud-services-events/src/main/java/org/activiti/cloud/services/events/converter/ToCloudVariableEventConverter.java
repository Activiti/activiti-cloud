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

import org.activiti.runtime.api.event.CloudVariableCreatedEvent;
import org.activiti.runtime.api.event.CloudVariableDeletedEvent;
import org.activiti.runtime.api.event.CloudVariableUpdatedEvent;
import org.activiti.runtime.api.event.VariableCreated;
import org.activiti.runtime.api.event.VariableDeleted;
import org.activiti.runtime.api.event.VariableUpdated;
import org.activiti.runtime.api.event.impl.CloudVariableCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudVariableDeletedEventImpl;
import org.activiti.runtime.api.event.impl.CloudVariableUpdatedEventImpl;

public class ToCloudVariableEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudVariableEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudVariableCreatedEvent from(VariableCreated event) {
        CloudVariableCreatedEventImpl cloudEvent = new CloudVariableCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudVariableUpdatedEvent from(VariableUpdated event) {
        CloudVariableUpdatedEventImpl cloudEvent = new CloudVariableUpdatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudVariableDeletedEvent from(VariableDeleted event) {
        CloudVariableDeletedEventImpl cloudEvent = new CloudVariableDeletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

}
