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

import org.activiti.api.runtime.model.impl.ApplicationElementImpl;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;

public class RuntimeBundleInfoAppender {

    private RuntimeBundleProperties properties;

    public RuntimeBundleInfoAppender(RuntimeBundleProperties properties) {
        this.properties = properties;
    }

    public CloudRuntimeEventImpl<?, ?> appendRuntimeBundleInfoTo(CloudRuntimeEventImpl<?, ?> cloudRuntimeEvent) {
        cloudRuntimeEvent.setAppName(properties.getAppName());
        cloudRuntimeEvent.setServiceName(properties.getServiceName());
        cloudRuntimeEvent.setServiceFullName(properties.getServiceFullName());
        cloudRuntimeEvent.setServiceType(properties.getServiceType());
        cloudRuntimeEvent.setServiceVersion(properties.getServiceVersion());

        return cloudRuntimeEvent;
    }

    public CloudRuntimeEntityImpl appendRuntimeBundleInfoTo(CloudRuntimeEntityImpl cloudRuntimeEntity) {
        cloudRuntimeEntity.setAppName(properties.getAppName());
        cloudRuntimeEntity.setServiceName(properties.getServiceName());
        cloudRuntimeEntity.setServiceFullName(properties.getServiceFullName());
        cloudRuntimeEntity.setServiceType(properties.getServiceType());
        cloudRuntimeEntity.setServiceVersion(properties.getServiceVersion());

        return cloudRuntimeEntity;
    }
}
