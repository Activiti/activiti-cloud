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
package org.activiti.cloud.qa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceDeploymentDescriptor {

    private String name;
    private String version;
    private ServiceType serviceType;

    public ServiceDeploymentDescriptor() {
    }

    public ServiceDeploymentDescriptor(String name,
                                       String version,
                                       ServiceType serviceType) {
        this.name = name;
        this.version = version;
        this.serviceType = serviceType;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }
}