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

import org.activiti.api.model.shared.event.VariableCreatedEvent;
import org.activiti.api.model.shared.event.VariableDeletedEvent;
import org.activiti.api.model.shared.event.VariableEvent.VariableEvents;
import org.activiti.api.model.shared.event.VariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.core.common.model.connector.VariableDefinition;
import org.activiti.spring.process.CachingProcessExtensionService;
import org.activiti.spring.process.model.Extension;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class ToCloudVariableEventConverter {

    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final CachingProcessExtensionService processExtensionService;

    public ToCloudVariableEventConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                         CachingProcessExtensionService processExtensionService) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.processExtensionService = processExtensionService;
    }

    public CloudVariableCreatedEvent from(VariableCreatedEvent event) {
        CloudVariableCreatedEventImpl cloudEvent = new CloudVariableCreatedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);

        cloudEvent.setVariableDefinitionId(getVariableDefinitionId(event));
        return cloudEvent;
    }

    public CloudVariableUpdatedEvent from(VariableUpdatedEvent event) {
        CloudVariableUpdatedEventImpl cloudEvent = new CloudVariableUpdatedEventImpl<>(event.getEntity(), event.getPreviousValue());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    public CloudVariableDeletedEvent from(VariableDeletedEvent event) {
        CloudVariableDeletedEventImpl cloudEvent = new CloudVariableDeletedEventImpl(event.getEntity());
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudEvent);
        return cloudEvent;
    }

    private String getVariableDefinitionId(VariableCreatedEvent event) {
        return Optional
            .ofNullable(event.getProcessDefinitionId())
            .map(processExtensionService::getExtensionsForId)
            .map(Extension::getProperties)
            .map(Map::values)
            .stream()
            .flatMap(Collection::stream)
            .filter(variableDefinition -> variableDefinition.getName() != null)
            .filter(variableDefinition -> variableDefinition.getName().equals(event.getEntity().getName()))
            .map(VariableDefinition::getId)
            .findFirst()
            .orElse(null);
    }

}
