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
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;

public class CloudIntegrationResultReceivedEventImpl
    extends CloudRuntimeEventImpl<IntegrationContext, IntegrationEvent.IntegrationEvents>
    implements CloudIntegrationResultReceivedEvent {

    private static final long serialVersionUID = 1L;

    public CloudIntegrationResultReceivedEventImpl() {}

    public CloudIntegrationResultReceivedEventImpl(IntegrationContext integrationContext) {
        super(integrationContext);
        if (getEntity() != null) {
            setEntityId(getEntity().getId());
        }

        setProcessInstanceId(integrationContext.getProcessInstanceId());
        setProcessDefinitionId(integrationContext.getProcessDefinitionId());
        setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
        setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
        setBusinessKey(integrationContext.getBusinessKey());
    }

    @Override
    public IntegrationEvent.IntegrationEvents getEventType() {
        return IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED;
    }
}
