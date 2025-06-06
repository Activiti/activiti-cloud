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
package org.activiti.cloud.examples;

import static org.activiti.cloud.examples.connectors.ExampleConnector.EXAMPLE_CONNECTOR_CONSUMER;
import static org.activiti.cloud.examples.connectors.HeadersConnector.HEADERS_CONNECTOR_CONSUMER;
import static org.activiti.cloud.examples.connectors.MoviesDescriptionConnector.MOVIES_DESCRIPTION_CONSUMER;
import static org.activiti.cloud.examples.connectors.MultiInstanceConnector.MULTI_INSTANCE_CONSUMER;
import static org.activiti.cloud.examples.connectors.TestBpmnErrorConnector.TEST_BPMN_ERROR_CONNECTOR_CONSUMER;
import static org.activiti.cloud.examples.connectors.TestErrorConnector.TEST_ERROR_CONNECTOR_CONSUMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.function.context.FunctionRegistration.REGISTRATION_NAME_SUFFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.activiti.cloud.examples.connectors.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = { CloudConnectorApp.class })
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test.properties")
public class CloudConnectorAppIT {

    private static final String CONNECTOR_SUFFIX = "Connector";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private FunctionCatalog functionCatalog;

    @Test
    public void contextShouldLoad() throws Exception {
        //then
        assertThat(context).isNotNull();
        assertThat(appName).isNotEmpty();

        assertThat(functionCatalog).isNotNull();
    }

    @Test
    public void functionCatalogContainsFunctionDefinitions() {
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(EXAMPLE_CONNECTOR_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(HEADERS_CONNECTOR_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(HEADERS_CONNECTOR_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(MOVIES_DESCRIPTION_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(MULTI_INSTANCE_CONSUMER))).isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(TEST_BPMN_ERROR_CONNECTOR_CONSUMER)))
            .isNotNull();
        assertThat(functionCatalog.<Object>lookup(getRegisteredConnectorName(TEST_ERROR_CONNECTOR_CONSUMER)))
            .isNotNull();
    }

    @Test
    public void shouldConvertExpectedJsonToPojo() throws IOException {
        String json = "{ \"test-json-variable-element1\":\"test-json-variable-value1\"}";
        Object jsonValue = objectMapper.readValue(json, Object.class);
        CustomPojo customPojo = objectMapper.convertValue(jsonValue, CustomPojo.class);
        assertThat(customPojo).isNotNull();
    }

    private static String getRegisteredConnectorName(String functionName) {
        return functionName + CONNECTOR_SUFFIX + REGISTRATION_NAME_SUFFIX;
    }
}
