/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.connectors.starter.test.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.CloudIntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.connectors.starter.ActivitiCloudConnectorApp;
import org.activiti.cloud.connectors.starter.channels.IntegrationRequestErrorChannelListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ActivitiCloudConnectorApp.class)
@ActiveProfiles(ConnectorsITStreamHandlers.CONNECTOR_IT)
@Import(TestChannelBinderConfiguration.class)
public class ActivitiCloudConnectorServiceIT {

    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";

    @Autowired
    private RuntimeMockStreams runtimeMockStreams;

    @Autowired
    private ConnectorsITStreamHandlers streamHandler;

    @Autowired
    private IntegrationRequestErrorChannelListener integrationRequestErrorChannelListener;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${activiti.cloud.application.name}")
    private String appName;

    @Value("${spring.application.name}")
    private String serviceFullName;

    @Value("${spring.cloud.stream.default.error-handler-definition}")
    private String defaultErrorHandlerDefinition;

    private static final String PROCESS_INSTANCE_ID = "processInstanceId-" + UUID.randomUUID().toString();
    private static final String PROCESS_DEFINITION_ID = "myProcessDefinitionId";
    private static final String INTEGRATION_ID = "integrationId-" + UUID.randomUUID().toString();

    @BeforeEach
    public void setUp() throws Exception {
        streamHandler.setIntegrationId(INTEGRATION_ID);
        ActivitiCloudConnectorApp.NULL_VARIABLES_RECEIVED_REQUEST.set(null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        streamHandler.reset();
        ActivitiCloudConnectorApp.NULL_VARIABLES_RECEIVED_REQUEST.set(null);
    }

    @Test
    public void integrationEventShouldBePickedByConnectorMock() throws Exception {
        //given

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "value1");
        variables.put("var2", 1L);

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(INTEGRATION_ID);
        integrationContext.setProcessInstanceId(PROCESS_INSTANCE_ID);
        integrationContext.setProcessDefinitionId(PROCESS_DEFINITION_ID);
        integrationContext.addInBoundVariables(variables);
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setAppName(appName);
        integrationRequest.setServiceFullName(serviceFullName);
        integrationRequest.setServiceType("runtime-bundle");
        integrationRequest.setServiceVersion("1");
        integrationRequest.setAppVersion("1");

        Message<IntegrationRequest> message = MessageBuilder
            .<IntegrationRequest>withPayload(integrationRequest)
            .setHeader("type", "Mock")
            .build();
        runtimeMockStreams.integrationEventsProducer().send(message);

        message =
            MessageBuilder
                .<IntegrationRequest>withPayload(integrationRequest)
                .setHeader("type", "MockProcessRuntime")
                .build();
        runtimeMockStreams.integrationEventsProducer().send(message);

        await("Should receive at least 2 integration results")
            .untilAsserted(() ->
                assertThat(streamHandler.getIntegrationResultEventsCounter().get()).isGreaterThanOrEqualTo(1)
            );
    }

    @Test
    public void integrationErrorShouldBeProducedByConnectorRuntimeExceptionMock() throws Exception {
        //given
        streamHandler.isIntegrationErrorEventProduced().set(false);

        IntegrationRequest integrationRequest = mockIntegrationRequest();

        Message<IntegrationRequest> message = MessageBuilder
            .withPayload(integrationRequest)
            .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
            .setHeader("type", "RuntimeException")
            .build();
        runtimeMockStreams.integrationEventsProducer().send(message);

        await("Should produce RuntimeException integration error")
            .untilTrue(streamHandler.isIntegrationErrorEventProduced());

        IntegrationError integrationError = streamHandler.getIntegrationError();

        assertThat(integrationError.getErrorClassName()).isEqualTo("java.lang.RuntimeException");
        assertThat(integrationError.getErrorMessage()).isEqualTo("Mock RuntimeException");
        assertThat(integrationError.getStackTraceElements()).asList().isNotEmpty();
        assertThat(integrationError.getIntegrationContext().getId()).isEqualTo(INTEGRATION_ID);
    }

    @Test
    public void integrationErrorShouldBeProducedByConnectorErrorMock() throws Exception {
        //given
        streamHandler.isIntegrationErrorEventProduced().set(false);

        IntegrationRequest integrationRequest = mockIntegrationRequest();

        Message<IntegrationRequest> message = MessageBuilder
            .withPayload(integrationRequest)
            .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
            .setHeader("type", "Error")
            .build();
        runtimeMockStreams.integrationEventsProducer().send(message);

        await("Should produce Error integration error").untilTrue(streamHandler.isIntegrationErrorEventProduced());

        IntegrationError integrationError = streamHandler.getIntegrationError();

        assertThat(integrationError.getErrorClassName()).isEqualTo("java.lang.Error");
        assertThat(integrationError.getErrorMessage()).isEqualTo("Mock Error");
        assertThat(integrationError.getStackTraceElements()).asList().isNotEmpty();
        assertThat(integrationError.getIntegrationContext().getId()).isEqualTo(INTEGRATION_ID);
    }

    @Test
    public void integrationErrorShouldBeProducedByConnectorCloudBpmnErrorCauseMock() throws Exception {
        //given
        streamHandler.isIntegrationErrorEventProduced().set(false);

        IntegrationRequest integrationRequest = mockIntegrationRequest();

        Message<IntegrationRequest> message = MessageBuilder
            .withPayload(integrationRequest)
            .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
            .setHeader("type", "CloudBpmnErrorCause")
            .build();

        runtimeMockStreams.integrationEventsProducer().send(message);

        await("Should produce CloudBpmnError with root cause and message integration error")
            .untilTrue(streamHandler.isIntegrationErrorEventProduced());

        IntegrationError integrationError = streamHandler.getIntegrationError();

        assertThat(integrationError.getErrorClassName()).isEqualTo(CloudBpmnError.class.getName());
        assertThat(integrationError.getErrorCode()).isEqualTo("ERROR_CODE");
        assertThat(integrationError.getErrorMessage()).isEqualTo("Error cause message");
        assertThat(integrationError.getStackTraceElements())
            .asList()
            .isNotEmpty()
            .extracting("methodName")
            .contains("raiseErrorCause")
            .anyMatch(element ->
                String.valueOf(element).matches(".*(mockTypeIntegrationCloudBpmnErrorRootCauseSender).*")
            );

        assertThat(integrationError.getIntegrationContext().getId()).isEqualTo(INTEGRATION_ID);
    }

    @Test
    public void integrationErrorShouldBeProducedByConnectorCloudBpmnErrorMessageMock() throws Exception {
        //given
        streamHandler.isIntegrationErrorEventProduced().set(false);

        IntegrationRequest integrationRequest = mockIntegrationRequest();

        Message<IntegrationRequest> message = MessageBuilder
            .withPayload(integrationRequest)
            .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
            .setHeader("type", "CloudBpmnErrorMessage")
            .build();

        runtimeMockStreams.integrationEventsProducer().send(message);

        await("Should produce CloudBpmnError with error code and message integration error")
            .untilTrue(streamHandler.isIntegrationErrorEventProduced());

        IntegrationError integrationError = streamHandler.getIntegrationError();

        assertThat(integrationError.getErrorClassName()).isEqualTo(CloudBpmnError.class.getName());
        assertThat(integrationError.getErrorCode()).isEqualTo("ERROR_CODE");
        assertThat(integrationError.getErrorMessage()).isEqualTo("Error code message");
        assertThat(integrationError.getStackTraceElements())
            .asList()
            .isNotEmpty()
            .extracting("methodName")
            .anyMatch(element -> String.valueOf(element).matches(".*(mockTypeIntegrationCloudBpmnErrorMessageSender).*")
            )
            .doesNotContain("raiseErrorCause");

        assertThat(integrationError.getIntegrationContext().getId()).isEqualTo(INTEGRATION_ID);
    }

    @Test
    public void integrationErrorShouldBeProducedByConnectorCloudBpmnErrorMock() throws Exception {
        //given
        streamHandler.isIntegrationErrorEventProduced().set(false);

        IntegrationRequest integrationRequest = mockIntegrationRequest();

        Message<IntegrationRequest> message = MessageBuilder
            .withPayload(integrationRequest)
            .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
            .setHeader("type", "CloudBpmnError")
            .build();

        runtimeMockStreams.integrationEventsProducer().send(message);

        await("Should produce CloudBpmnError integration error")
            .untilTrue(streamHandler.isIntegrationErrorEventProduced());

        IntegrationError integrationError = streamHandler.getIntegrationError();

        assertThat(integrationError.getErrorClassName()).isEqualTo(CloudBpmnError.class.getName());
        assertThat(integrationError.getErrorCode()).isEqualTo("ERROR_CODE");
        assertThat(integrationError.getErrorMessage()).isEqualTo("ERROR_CODE");
        assertThat(integrationError.getStackTraceElements())
            .asList()
            .isNotEmpty()
            .extracting("methodName")
            .anyMatch(element -> String.valueOf(element).matches(".*(mockTypeIntegrationCloudBpmnErrorSender).*"))
            .doesNotContain("raiseErrorCause");

        assertThat(integrationError.getIntegrationContext().getId()).isEqualTo(INTEGRATION_ID);
    }

    @Test
    void defaultErrorHandlerDefinition() {
        assertThat(defaultErrorHandlerDefinition).isEqualTo("integrationRequestErrorChannelListener");
    }

    @Test
    void integrationRequestErrorChannelListener() throws JacksonException {
        streamHandler.isIntegrationErrorEventProduced().set(false);

        //given
        IntegrationRequest integrationRequest = mockIntegrationRequest();

        var errorMessage = new ErrorMessage(
            new MessagingException(
                MessageBuilder
                    .withPayload(objectMapper.writeValueAsBytes(integrationRequest))
                    .setHeader(INTEGRATION_CONTEXT_ID, UUID.randomUUID().toString())
                    .build(),
                new RuntimeException("Unexpected exception")
            )
        );

        //when
        integrationRequestErrorChannelListener.accept(errorMessage);

        //then
        await("Should produce integration error message").untilTrue(streamHandler.isIntegrationErrorEventProduced());

        var integrationError = streamHandler.getIntegrationError();

        assertThat(integrationError).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variablesFragments")
    void should_preserveNonNullVariables_whenDeserializingCloudIntegrationContext(
        String label,
        String variablesFragment
    ) {
        //given
        String idpPayload =
            """
            {
              "id": "da504fe5-479d-11f1-95ff-4afa9ca877cc",
              %s
              "processInstanceId": "d992cc3f-479d-11f1-95ff-4afa9ca877cc",
              "processDefinitionId": "Process_1:2:e9a4af07",
              "processDefinitionKey": "Process_1",
              "processDefinitionVersion": 2,
              "clientId": "hxpIdpConnector",
              "clientType": "ServiceTask",
              "connectorType": "my-classification.CLASSIFICATION"
            }
            """.formatted(
                    variablesFragment
                );

        //when
        CloudIntegrationContextImpl context = objectMapper.readValue(
            idpPayload.getBytes(),
            CloudIntegrationContextImpl.class
        );

        //then
        assertThat(context.getInBoundVariables())
            .as("inBoundVariables should be empty map after deserialization, not null")
            .isNotNull()
            .isEmpty();
        assertThat(context.getOutBoundVariables())
            .as("outBoundVariables should be empty map after deserialization, not null")
            .isNotNull()
            .isEmpty();
    }

    @Test
    void should_receiveNonNullVariables_whenIntegrationRequestHasNullVariables() {
        //given
        String json = objectMapper.writeValueAsString(this.mockIntegrationRequest());
        json =
            json
                .replace("\"inBoundVariables\":{}", "\"inBoundVariables\":null")
                .replace("\"outBoundVariables\":{}", "\"outBoundVariables\":null");

        // when
        Message<byte[]> message = MessageBuilder
            .withPayload(json.getBytes())
            .setHeader("type", "NullVariables")
            .setHeader("contentType", "application/json")
            .build();
        runtimeMockStreams.integrationEventsProducer().send(message);

        //then
        await("Connector should process the message with non-null variables")
            .untilAtomic(ActivitiCloudConnectorApp.NULL_VARIABLES_RECEIVED_REQUEST, notNullValue());

        IntegrationRequest receivedRequest = ActivitiCloudConnectorApp.NULL_VARIABLES_RECEIVED_REQUEST.get();
        assertThat(receivedRequest.getIntegrationContext().getInBoundVariables())
            .as("inBoundVariables should be non-null empty map, not null")
            .isNotNull()
            .isEmpty();
        assertThat(receivedRequest.getIntegrationContext().getOutBoundVariables())
            .as("outBoundVariables should be non-null empty map, not null")
            .isNotNull()
            .isEmpty();
    }

    static Stream<Arguments> variablesFragments() {
        return Stream.of(
            Arguments.of("null variables", "\"inBoundVariables\": null, \"outBoundVariables\": null,"),
            Arguments.of("absent variables", "")
        );
    }

    private IntegrationRequest mockIntegrationRequest() {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(INTEGRATION_ID);
        integrationContext.setProcessInstanceId(PROCESS_INSTANCE_ID);
        integrationContext.setProcessDefinitionId(PROCESS_DEFINITION_ID);

        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setAppName(appName);
        integrationRequest.setServiceFullName(serviceFullName);
        integrationRequest.setServiceType("runtime-bundle");
        integrationRequest.setServiceVersion("1");
        integrationRequest.setAppVersion("1");

        return integrationRequest;
    }
}
