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

package org.activiti.cloud.api.model.shared;

public interface CloudRuntimeEntity {

    /**
     * Application name, for a runtime-bundle it is the value of the <i>activiti.cloud.application.name</i> spring property.
     */
    String getAppName();

    /**
     * Application version, for a runtime-bundle it is the value of the <i>activiti.cloud.application.version</i> spring property.
     */
    String getAppVersion();

    /**
     * Service name, for a runtime-bundle it is the value of the <i>spring.application.name</i> spring property.
     */
    String getServiceName();

    /**
     * Service full name, at the moment it is the same as serviceName.
     */
    String getServiceFullName();

    /**
     * Service type, for a runtime-bundle it is the value of the <i>activiti.cloud.service.type</i> spring property.
     */
    String getServiceType();

    /**
     * Service version, for a runtime-bundle it is the value of the <i>activiti.cloud.service.version</i> spring property.
     */
    String getServiceVersion();

}
