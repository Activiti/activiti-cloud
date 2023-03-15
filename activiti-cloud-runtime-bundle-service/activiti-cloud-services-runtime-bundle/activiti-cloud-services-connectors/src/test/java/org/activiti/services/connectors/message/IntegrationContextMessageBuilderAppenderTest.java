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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationContextMessageBuilderAppenderTest {

    private static final String PARENT_PROCESS_INSTANCE_ID = "parentProcessInstanceId";
    private static final String ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";
    private static final String PROCESS_INSTANCE_ID = "processInstanceId";
    private static final int _1 = 1;
    private static final String PROCESS_DEFINITION_KEY = "processDefinitionKey";
    private static final String PROCESS_DEFINITION_ID = "processDefinitionId";
    private static final String CONNECTOR_TYPE = "connectorType";
    private static final String BUSINESS_KEY = "businessKey";
    private static final String ID = "id";
    private static final String APP_VERSION = "1";

    private IntegrationContextMessageBuilderAppender integrationBuilder;

    private IntegrationContextImpl integrationContext;

    @BeforeEach
    public void setUp() {
        integrationContext = anIntegrationContext();
        integrationBuilder = new IntegrationContextMessageBuilderAppender(integrationContext);
    }

    @Test
    public void testApply() {
        // given
        MessageBuilder<IntegrationRequest> request = MessageBuilder.withPayload(new IntegrationRequestImpl());

        // when
        integrationBuilder.apply(request);

        // then
        Message<IntegrationRequest> message = request.build();

        assertThat(message.getHeaders())
            .containsEntry(IntegrationContextMessageHeaders.CONNECTOR_TYPE, integrationContext.getConnectorType())
            .containsEntry(IntegrationContextMessageHeaders.BUSINESS_KEY, integrationContext.getBusinessKey())
            .containsEntry(IntegrationContextMessageHeaders.INTEGRATION_CONTEXT_ID, integrationContext.getId())
            .containsEntry(
                IntegrationContextMessageHeaders.ROOT_PROCESS_INSTANCE_ID,
                integrationContext.getRootProcessInstanceId()
            )
            .containsEntry(
                IntegrationContextMessageHeaders.PROCESS_INSTANCE_ID,
                integrationContext.getProcessInstanceId()
            )
            .containsEntry(
                IntegrationContextMessageHeaders.PROCESS_DEFINITION_ID,
                integrationContext.getProcessDefinitionId()
            )
            .containsEntry(
                IntegrationContextMessageHeaders.PROCESS_DEFINITION_KEY,
                integrationContext.getProcessDefinitionKey()
            )
            .containsEntry(
                IntegrationContextMessageHeaders.PROCESS_DEFINITION_VERSION,
                integrationContext.getProcessDefinitionVersion()
            )
            .containsEntry(IntegrationContextMessageHeaders.APP_VERSION, integrationContext.getAppVersion())
            .containsEntry(
                IntegrationContextMessageHeaders.PARENT_PROCESS_INSTANCE_ID,
                integrationContext.getParentProcessInstanceId()
            );
    }

    private IntegrationContextImpl anIntegrationContext() {
        integrationContext = new IntegrationContextImpl();

        integrationContext.setId(ID);
        integrationContext.setBusinessKey(BUSINESS_KEY);
        integrationContext.setConnectorType(CONNECTOR_TYPE);
        integrationContext.setProcessDefinitionId(PROCESS_DEFINITION_ID);
        integrationContext.setProcessDefinitionKey(PROCESS_DEFINITION_KEY);
        integrationContext.setProcessDefinitionVersion(_1);
        integrationContext.setRootProcessInstanceId(ROOT_PROCESS_INSTANCE_ID);
        integrationContext.setProcessInstanceId(PROCESS_INSTANCE_ID);
        integrationContext.setAppVersion(APP_VERSION);
        integrationContext.setParentProcessInstanceId(PARENT_PROCESS_INSTANCE_ID);

        return integrationContext;
    }
}
