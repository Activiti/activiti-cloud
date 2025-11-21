/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.events.handlers;

import java.util.Date;
import java.util.UUID;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public abstract class IntegrationEventsHelper {

    protected static final String PROCESS_INSTANCE_ID = UUID.randomUUID().toString();
    protected static final String CLIENT_ID = UUID.randomUUID().toString();
    protected static final String EXECUTION_ID = UUID.randomUUID().toString();

    protected String getLegacyId() {
        return PROCESS_INSTANCE_ID + ":" + CLIENT_ID + ":" + EXECUTION_ID;
    }

    protected IntegrationContextImpl createIntegrationContext(String integrationContextId) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(integrationContextId);
        integrationContext.setProcessInstanceId(PROCESS_INSTANCE_ID);
        integrationContext.setClientId(CLIENT_ID);
        integrationContext.setExecutionId(EXECUTION_ID);
        return integrationContext;
    }

    protected IntegrationContextEntity createIntegrationContextEntity(String id) {
        IntegrationContextEntity existingEntity = new IntegrationContextEntity(
            "serviceName",
            "serviceFullName",
            "serviceVersion",
            "appName",
            "appVersion"
        );
        existingEntity.setId(id);
        existingEntity.setProcessInstanceId(PROCESS_INSTANCE_ID);
        existingEntity.setClientId(CLIENT_ID);
        existingEntity.setExecutionId(EXECUTION_ID);
        existingEntity.setStatus(CloudIntegrationContext.IntegrationContextStatus.INTEGRATION_REQUESTED);
        existingEntity.setRequestDate(new Date());

        return existingEntity;
    }

    protected ServiceTaskEntity createServiceTaskEntity(String id) {
        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity(
            "serviceName",
            "serviceFullName",
            "serviceVersion",
            "appName",
            "appVersion"
        );
        serviceTaskEntity.setId(id);
        serviceTaskEntity.setProcessInstanceId(PROCESS_INSTANCE_ID);
        serviceTaskEntity.setExecutionId(EXECUTION_ID);
        serviceTaskEntity.setElementId(CLIENT_ID);
        serviceTaskEntity.setStatus(CloudBPMNActivity.BPMNActivityStatus.STARTED);
        serviceTaskEntity.setStartedDate(new Date());

        return serviceTaskEntity;
    }
}
