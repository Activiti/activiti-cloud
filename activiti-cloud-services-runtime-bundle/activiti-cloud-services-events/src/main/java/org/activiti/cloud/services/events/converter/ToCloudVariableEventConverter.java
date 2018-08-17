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

import org.activiti.api.model.shared.event.VariableCreatedEvent;
import org.activiti.api.model.shared.event.VariableDeletedEvent;
import org.activiti.api.model.shared.event.VariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;

public class ToCloudVariableEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudVariableEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudVariableCreatedEvent from(VariableCreatedEvent event) {
        CloudVariableCreatedEventImpl cloudEvent = new CloudVariableCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudVariableUpdatedEvent from(VariableUpdatedEvent event) {
        CloudVariableUpdatedEventImpl cloudEvent = new CloudVariableUpdatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudVariableDeletedEvent from(VariableDeletedEvent event) {
        CloudVariableDeletedEventImpl cloudEvent = new CloudVariableDeletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

}
