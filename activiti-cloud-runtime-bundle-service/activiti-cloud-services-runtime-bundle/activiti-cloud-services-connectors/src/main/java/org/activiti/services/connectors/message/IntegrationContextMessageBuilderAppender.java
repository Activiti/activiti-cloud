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
package org.activiti.services.connectors.message;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.services.events.message.MessageBuilderAppender;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class IntegrationContextMessageBuilderAppender implements MessageBuilderAppender {

    private final IntegrationContext integrationContext;

    public IntegrationContextMessageBuilderAppender(IntegrationContext integrationContext) {
        Assert.notNull(integrationContext, "integrationContext must not be null");

        this.integrationContext = integrationContext;
    }

    @Override
    public <T> MessageBuilder<T> apply(MessageBuilder<T> request) {
        return request
            .setHeader(IntegrationContextMessageHeaders.CONNECTOR_TYPE, integrationContext.getConnectorType())
            .setHeader(IntegrationContextMessageHeaders.BUSINESS_KEY, integrationContext.getBusinessKey())
            .setHeader(IntegrationContextMessageHeaders.INTEGRATION_CONTEXT_ID, integrationContext.getId())
            .setHeader(
                IntegrationContextMessageHeaders.ROOT_PROCESS_INSTANCE_ID,
                integrationContext.getRootProcessInstanceId()
            )
            .setHeader(IntegrationContextMessageHeaders.PROCESS_INSTANCE_ID, integrationContext.getProcessInstanceId())
            .setHeader(IntegrationContextMessageHeaders.EXECUTION_ID, integrationContext.getExecutionId())
            .setHeader(
                IntegrationContextMessageHeaders.PROCESS_DEFINITION_ID,
                integrationContext.getProcessDefinitionId()
            )
            .setHeader(
                IntegrationContextMessageHeaders.PROCESS_DEFINITION_KEY,
                integrationContext.getProcessDefinitionKey()
            )
            .setHeader(
                IntegrationContextMessageHeaders.PROCESS_DEFINITION_VERSION,
                integrationContext.getProcessDefinitionVersion()
            )
            .setHeader(
                IntegrationContextMessageHeaders.PARENT_PROCESS_INSTANCE_ID,
                integrationContext.getParentProcessInstanceId()
            )
            .setHeader(IntegrationContextMessageHeaders.APP_VERSION, integrationContext.getAppVersion());
    }
}
