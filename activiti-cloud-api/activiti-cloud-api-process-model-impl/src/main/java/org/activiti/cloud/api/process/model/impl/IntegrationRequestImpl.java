/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.api.process.model.impl;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.IntegrationRequest;

public class IntegrationRequestImpl extends CloudRuntimeEntityImpl implements IntegrationRequest {

    private IntegrationContext integrationContext;

    public IntegrationRequestImpl() {
    }

    public IntegrationRequestImpl(IntegrationContext integrationContext) {
        this.integrationContext = integrationContext;
        this.setAppVersion(integrationContext.getAppVersion());
    }

    @Override
    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }
}
