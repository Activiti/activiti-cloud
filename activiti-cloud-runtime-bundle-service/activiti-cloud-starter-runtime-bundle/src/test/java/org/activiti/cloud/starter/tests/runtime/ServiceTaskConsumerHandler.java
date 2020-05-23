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
package org.activiti.cloud.starter.tests.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.support.MessageBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@TestComponent
@EnableBinding(ConnectorIntegrationChannels.class)
public class ServiceTaskConsumerHandler {

    private static final String PARENT_PROCESS_INSTANCE_ID = "parentProcessInstanceId";
    private static final String SERVICE_FULL_NAME = "serviceFullName";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String SERVICE_NAME = "serviceName";
    private static final String APP_NAME = "appName";
    private static final String APP_VERSION = "appVersion";
    private static final String PROCESS_DEFINITION_ID = "processDefinitionId";
    private static final String PROCESS_INSTANCE_ID = "processInstanceId";
    private static final String INTEGRATION_CONTEXT_ID = "integrationContextId";
    private static final String CONNECTOR_TYPE = "connectorType";
    private static final String BUSINESS_KEY = "businessKey";
    private static final String PROCESS_DEFINITION_KEY = "processDefinitionKey";
    private static final String PROCESS_DEFINITION_VERSION = "processDefinitionVersion";
    private static final String MESSAGE_PAYLOAD_TYPE = "messagePayloadType";
    private static final String ROUTING_KEY = "routingKey";

    private final BinderAwareChannelResolver resolver;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public ServiceTaskConsumerHandler(BinderAwareChannelResolver resolver,
        RuntimeBundleProperties runtimeBundleProperties,
        ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.objectMapper = objectMapper;
    }

    @StreamListener(value = ConnectorIntegrationChannels.INTEGRATION_EVENTS_CONSUMER)
    public void receive(IntegrationRequest integrationRequest, @Headers Map<String, Object> headers) {
        assertIntegrationContextHeaders(integrationRequest, headers);

        IntegrationContext integrationContext = integrationRequest.getIntegrationContext();

        Map<String, Object> requestVariables = integrationContext.getInBoundVariables();

        Object customPojo = requestVariables.get("customPojo");

        String variableToUpdate = "age";

        HashMap<String, Object> resultVariables = new HashMap<>();
        resultVariables.put(variableToUpdate,
            ((Integer) requestVariables.get(variableToUpdate)) + 1);
        //invert value of boolean
        resultVariables.put("boolVar", !(Boolean) requestVariables.get("boolVar"));

        resultVariables.put("customPojoTypeInConnector", "Type of customPojo var in connector is " + customPojo.getClass());
        resultVariables.put("customPojoField1InConnector", "Value of field1 on customPojo is " + objectMapper.convertValue(customPojo, CustomPojo.class).getField1());
        //even the annotated pojo in connector won't be deserialized as the relevant type unless we tell objectMapper to do so
        resultVariables.put("customPojoAnnotatedTypeInConnector", "Type of customPojoAnnotated var in connector is " + requestVariables.get("customPojoAnnotated").getClass());

        integrationContext.addOutBoundVariables(resultVariables);

        IntegrationResultImpl integrationResult = new IntegrationResultImpl(integrationRequest, integrationContext);
        Message<IntegrationResultImpl> message = MessageBuilder.withPayload(integrationResult).build();
        resolver.resolveDestination("integrationResult_" + runtimeBundleProperties.getServiceFullName()).send(message);
    }

    @StreamListener(value = ConnectorIntegrationChannels.VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER)
    public void receiveVariablesConnector(IntegrationRequest integrationRequest, @Headers Map<String, Object> headers) throws Exception {
        assertIntegrationContextHeaders(integrationRequest, headers);

        IntegrationContext integrationContext = integrationRequest.getIntegrationContext();

        Map<String, Object> inBoundVariables = integrationContext.getInBoundVariables();
        String variableOne = "input_variable_name_1";
        String variableTwo = "input_variable_name_2";
        String variableThree = "input_variable_name_3";
        String constant = "_constant_value_";

        Integer currentAge = (Integer) inBoundVariables.get(variableTwo);
        Integer offSet = (Integer) inBoundVariables.get(variableThree);

        assertThat(inBoundVariables.entrySet())
            .extracting(Map.Entry::getKey,
                Map.Entry::getValue)
            .containsOnly(
                tuple(variableOne,
                    "inName"),
                tuple(variableTwo,
                    20),
                tuple(variableThree,
                    5),
                tuple(constant,
                    "myConstantValue"));

        integrationContext.addOutBoundVariable("out_variable_name_1",
            "outName");
        integrationContext.addOutBoundVariable("out_variable_name_2",
            currentAge + offSet);
        integrationContext.addOutBoundVariable("out_unmapped_variable_matching_name",
            "outTest");
        integrationContext.addOutBoundVariable("out_unmapped_variable_non_matching_name",
            "outTest");

        JsonNode value = new ObjectMapper().readTree("{\n"
            + "  \"city\": {\n"
            + "    \"name\": \"London\",\n"
            + "    \"place\": \"Tower of London\"\n"
            + "  }\n"
            + "}");
        integrationContext.addOutBoundVariable("sightSeeing", value);
        integrationContext.addOutBoundVariable("visitors", Arrays.asList("Peter", "Paul", "Jack"));

        IntegrationResultImpl integrationResult = new IntegrationResultImpl(integrationRequest, integrationContext);
        Message<IntegrationResultImpl> message = MessageBuilder.withPayload(integrationResult).build();
        resolver.resolveDestination("integrationResult_" + runtimeBundleProperties.getServiceFullName()).send(message);
    }

    @StreamListener(value = ConnectorIntegrationChannels.CONSTANTS_INTEGRATION_EVENTS_CONSUMER)
    public void receiveConstantsConnector(IntegrationRequest integrationRequest,
        @Headers Map<String, Object> headers) {
        assertIntegrationContextHeaders(integrationRequest, headers);
        IntegrationContext integrationContext = integrationRequest.getIntegrationContext();
        Map<String, Object> inBoundVariables = integrationContext.getInBoundVariables();

        Object constantValue = inBoundVariables.get("_constant_value_");

        assertThat(inBoundVariables.entrySet())
            .extracting(Map.Entry::getKey,
                Map.Entry::getValue)
            .containsOnly(tuple("name",
                "inName"),
                tuple("age",
                    20),
                tuple("_constant_value_",
                    "myConstantValue"));

        integrationContext.addOutBoundVariable("name", "outName");
        integrationContext.addOutBoundVariable("age", 25);
        integrationContext.addOutBoundVariable("_constant_value_", constantValue);

        IntegrationResultImpl integrationResult = new IntegrationResultImpl(integrationRequest, integrationContext);
        Message<IntegrationResultImpl> message = MessageBuilder.withPayload(integrationResult).build();
        resolver.resolveDestination("integrationResult_" + runtimeBundleProperties.getServiceFullName()).send(message);
    }

    @StreamListener(value = ConnectorIntegrationChannels.REST_CONNECTOR_CONSUMER,
        condition = "headers['processDefinitionVersion']!=null")
    public void receiveRestConnector(IntegrationRequest integrationRequest, @Headers Map<String, Object> headers) {
        assertIntegrationContextHeaders(integrationRequest, headers);

        IntegrationContext integrationContext = integrationRequest.getIntegrationContext();
        integrationContext.addOutBoundVariable("restResult", "fromConnector");

        IntegrationResultImpl integrationResult = new IntegrationResultImpl(integrationRequest, integrationContext);
        Message<IntegrationResultImpl> message = MessageBuilder.withPayload(integrationResult).build();
        resolver.resolveDestination("integrationResult_" + runtimeBundleProperties.getServiceFullName()).send(message);
    }


    private void assertIntegrationContextHeaders(IntegrationRequest integrationRequest, Map<String, Object> headers) {
        IntegrationContext integrationContext = integrationRequest.getIntegrationContext();

        // Mandatory headers assertions
        Assertions.assertThat(headers)
            .containsKey(ROUTING_KEY)
            .containsKey(MESSAGE_PAYLOAD_TYPE)
            .containsEntry(PROCESS_DEFINITION_VERSION, integrationContext.getProcessDefinitionVersion())
            .containsEntry(PROCESS_DEFINITION_KEY, integrationContext.getProcessDefinitionKey())
            .containsEntry(CONNECTOR_TYPE, integrationContext.getConnectorType())
            .containsEntry(INTEGRATION_CONTEXT_ID, integrationContext.getId())
            .containsEntry(PROCESS_INSTANCE_ID, integrationContext.getProcessInstanceId())
            .containsEntry(PROCESS_DEFINITION_ID, integrationContext.getProcessDefinitionId())
            .containsEntry(APP_NAME, integrationRequest.getAppName())
            .containsEntry(APP_VERSION, integrationRequest.getIntegrationContext().getAppVersion())
            .containsEntry(SERVICE_NAME, integrationRequest.getServiceName())
            .containsEntry(SERVICE_TYPE, integrationRequest.getServiceType())
            .containsEntry(SERVICE_VERSION, integrationRequest.getServiceVersion())
            .containsEntry(SERVICE_FULL_NAME, integrationRequest.getServiceFullName());

        // conditional on existing businessKey in integration context
        if (integrationContext.getBusinessKey() != null) {
            Assertions.assertThat(headers)
                .containsEntry(BUSINESS_KEY, integrationContext.getBusinessKey());
        }

        // conditional on existing parentProcessInstanceId in integration context
        if (integrationContext.getParentProcessInstanceId() != null) {
            Assertions.assertThat(headers)
                .containsEntry(PARENT_PROCESS_INSTANCE_ID, integrationContext.getParentProcessInstanceId());
        }
    }

}
