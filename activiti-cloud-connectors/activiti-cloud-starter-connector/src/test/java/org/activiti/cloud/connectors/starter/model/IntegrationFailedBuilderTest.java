package org.activiti.cloud.connectors.starter.model;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.events.CloudIntegrationExecutedEvent;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class IntegrationFailedBuilderTest {

    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String CONNECTOR_TYPE = "connectorType";
    private static final String BUSINESS_KEY = "businessKey";
    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";
    private static final String EXEC_ID = "executionId";
    private static final String PROC_DEF_KEY = "procDefKey";
    private static final Integer PROC_DEF_VERSION = 5;
    private static final String PARENT_PROC_INST_ID = "parentProcInstId";
    private static final String APP_VERSION = "appVersion";

    private static final String PREFIX = "config_";

    private ConnectorProperties properties = new ConnectorProperties();

    @Test
    public void shouldBuildMessageWithHeaders() throws Exception {
        Throwable error = new Error("Boom!");

        properties.setAppName(PREFIX+IntegrationMessageHeaders.APP_NAME);
        properties.setServiceName(PREFIX+IntegrationMessageHeaders.SERVICE_NAME);
        properties.setServiceType(PREFIX+IntegrationMessageHeaders.SERVICE_TYPE);
        properties.setServiceVersion(PREFIX+IntegrationMessageHeaders.SERVICE_VERSION);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setConnectorType(CONNECTOR_TYPE);
        integrationContext.setBusinessKey(BUSINESS_KEY);
        integrationContext.setId(INTEGRATION_CONTEXT_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.setExecutionId(EXEC_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessDefinitionKey(PROC_DEF_KEY);
        integrationContext.setProcessDefinitionVersion(PROC_DEF_VERSION);
        integrationContext.setParentProcessInstanceId(PARENT_PROC_INST_ID);
        integrationContext.setAppVersion(APP_VERSION);

        IntegrationRequest integrationRequestEvent = new IntegrationRequestImpl(integrationContext);
        IntegrationResult integrationResult = new IntegrationResultImpl(integrationRequestEvent, integrationContext);

        Message<CloudRuntimeEvent<?, ?>[]> message = IntegrationExecutedBuilder.executionFor(integrationResult, properties).buildMessage();

        Assertions.assertThat(message.getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, "application/json");
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.CONNECTOR_TYPE, CONNECTOR_TYPE);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.BUSINESS_KEY, BUSINESS_KEY);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.INTEGRATION_CONTEXT_ID, INTEGRATION_CONTEXT_ID);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.PROCESS_INSTANCE_ID, PROC_INST_ID);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.EXECUTION_ID, EXEC_ID);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.PROCESS_DEFINITION_ID, PROC_DEF_ID);
        Assertions.assertThat(message.getHeaders())
            .containsEntry(IntegrationMessageHeaders.PROCESS_DEFINITION_KEY, PROC_DEF_KEY);
        Assertions.assertThat(message.getHeaders())
            .containsEntry(IntegrationMessageHeaders.PROCESS_DEFINITION_VERSION, PROC_DEF_VERSION);
        Assertions.assertThat(message.getHeaders())
            .containsEntry(IntegrationMessageHeaders.PARENT_PROCESS_INSTANCE_ID, PARENT_PROC_INST_ID);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.APP_VERSION, APP_VERSION);
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.APP_NAME, properties.getAppName());
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.SERVICE_NAME, properties.getServiceName());
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.SERVICE_FULL_NAME, properties.getServiceFullName());
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.SERVICE_TYPE, properties.getServiceType());
        Assertions.assertThat(message.getHeaders()).containsEntry(IntegrationMessageHeaders.SERVICE_VERSION, properties.getServiceVersion());

        Assertions.assertThat(message.getPayload()[0]).isInstanceOf(CloudIntegrationExecutedEvent.class);
    }
}
