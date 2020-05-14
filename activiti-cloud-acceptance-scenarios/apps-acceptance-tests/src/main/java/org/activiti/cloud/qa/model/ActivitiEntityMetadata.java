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

public class ActivitiEntityMetadata {
    protected String serviceName;
    protected String serviceFullName;
    protected String serviceVersion;
    protected String appName;
    protected String appVersion;

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }
}
