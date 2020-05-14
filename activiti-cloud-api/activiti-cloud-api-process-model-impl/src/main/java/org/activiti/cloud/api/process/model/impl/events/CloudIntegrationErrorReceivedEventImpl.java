/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.api.process.model.impl.events;

import java.util.List;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;

public class CloudIntegrationErrorReceivedEventImpl extends CloudRuntimeEventImpl<IntegrationContext, IntegrationEvent.IntegrationEvents>
        implements CloudIntegrationErrorReceivedEvent {

    private static final long serialVersionUID = 1L;

    private String errorMessage;
    private String errorClassName;
    private List<StackTraceElement> stackTraceElements;

    public CloudIntegrationErrorReceivedEventImpl() {
    }

    public CloudIntegrationErrorReceivedEventImpl(IntegrationContext integrationContext,
                                                  String errorMessage,
                                                  String errorClassName,
                                                  List<StackTraceElement> stackTraceElements) {
        super(integrationContext);
        if (getEntity() != null) {
            setEntityId(getEntity().getId());
        }

        setProcessInstanceId(integrationContext.getProcessInstanceId());
        setProcessDefinitionId(integrationContext.getProcessDefinitionId());
        setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
        setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
        setBusinessKey(integrationContext.getBusinessKey());

        this.errorMessage = errorMessage;
        this.errorClassName = errorClassName;
        this.stackTraceElements = stackTraceElements;
    }

    @Override
    public IntegrationEvent.IntegrationEvents getEventType() {
        return IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getErrorClassName() {
        return errorClassName;
    }

    @Override
    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }
}
