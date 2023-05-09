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
package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;

public abstract class CloudIntegrationEventImpl
    extends CloudRuntimeEventImpl<IntegrationContext, IntegrationEvent.IntegrationEvents> {

    protected static final long serialVersionUID = 1L;

    protected CloudIntegrationEventImpl() {}

    protected CloudIntegrationEventImpl(IntegrationContext integrationContext) {
        super(integrationContext);
        setFieldValues(integrationContext);
    }

    protected CloudIntegrationEventImpl(String id, Long timestamp, IntegrationContext integrationContext) {
        super(id, timestamp, integrationContext);
        setFieldValues(integrationContext);
    }

    protected void setFieldValues(IntegrationContext integrationContext) {
        if (getEntity() != null) {
            setEntityId(getEntity().getId());
        }

        setProcessInstanceId(integrationContext.getProcessInstanceId());
        setProcessDefinitionId(integrationContext.getProcessDefinitionId());
        setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
        setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
        setBusinessKey(integrationContext.getBusinessKey());
    }
}
