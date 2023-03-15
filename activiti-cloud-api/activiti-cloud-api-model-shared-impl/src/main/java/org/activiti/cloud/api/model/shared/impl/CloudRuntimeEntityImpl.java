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
package org.activiti.cloud.api.model.shared.impl;

import java.util.Objects;
import org.activiti.api.model.shared.model.ApplicationElement;
import org.activiti.api.runtime.model.impl.ApplicationElementImpl;
import org.activiti.cloud.api.model.shared.CloudRuntimeEntity;

public class CloudRuntimeEntityImpl extends ApplicationElementImpl implements CloudRuntimeEntity {

    private String appName;
    private String serviceName;
    private String serviceFullName;
    private String serviceType;
    private String serviceVersion;

    public CloudRuntimeEntityImpl() {}

    public CloudRuntimeEntityImpl(ApplicationElement applicationElement) {
        setAppVersion(applicationElement.getAppVersion());
    }

    @Override
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(appName, serviceFullName, serviceName, serviceType, serviceVersion);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudRuntimeEntityImpl other = (CloudRuntimeEntityImpl) obj;
        return (
            Objects.equals(appName, other.appName) &&
            Objects.equals(serviceFullName, other.serviceFullName) &&
            Objects.equals(serviceName, other.serviceName) &&
            Objects.equals(serviceType, other.serviceType) &&
            Objects.equals(serviceVersion, other.serviceVersion)
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudRuntimeEntityImpl [appName=")
            .append(appName)
            .append(", serviceName=")
            .append(serviceName)
            .append(", serviceFullName=")
            .append(serviceFullName)
            .append(", serviceType=")
            .append(serviceType)
            .append(", serviceVersion=")
            .append(serviceVersion)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
