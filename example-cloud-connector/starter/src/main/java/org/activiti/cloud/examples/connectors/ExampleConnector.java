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
package org.activiti.cloud.examples.connectors;

import static net.logstash.logback.marker.Markers.append;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ExampleConnectorChannels.class)
public class ExampleConnector {

    private final Logger logger = LoggerFactory.getLogger(ExampleConnector.class);

    @Value("${spring.application.name}")
    private String appName;

    //just a convenience - not recommended in real implementations
    private String var1Copy = "";

    @Autowired
    private ConnectorProperties connectorProperties;

    private final ObjectMapper objectMapper;

    private final IntegrationResultSender integrationResultSender;

    public ExampleConnector(IntegrationResultSender integrationResultSender, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.integrationResultSender = integrationResultSender;
    }

    @StreamListener(value = ExampleConnectorChannels.EXAMPLE_CONNECTOR_CONSUMER)
    public void performTask(IntegrationRequest event) throws InterruptedException {
        logger.info(append("service-name", appName), ">>> In example-cloud-connector");

        String var1 =
            ExampleConnector.class.getSimpleName() +
            " was called for instance " +
            event.getIntegrationContext().getProcessInstanceId();

        var1Copy = String.valueOf(var1);

        Object jsonVar = event.getIntegrationContext().getInBoundVariables().get("test_json_variable_name");
        Object longJsonVar = event.getIntegrationContext().getInBoundVariables().get("test_long_json_variable_name");

        Map<String, Object> results = new HashMap<>();

        if (jsonVar != null) {
            logger.info("jsonVar value type " + jsonVar.getClass().getTypeName());
            logger.info("jsonVar value as string " + jsonVar.toString());

            CustomPojo customPojo = objectMapper.convertValue(jsonVar, CustomPojo.class);
            results.put(
                "test_json_variable_result",
                "able to convert test_json_variable_name to " + CustomPojo.class.getName()
            );
        }

        if (longJsonVar != null && longJsonVar instanceof LinkedHashMap) {
            if (((LinkedHashMap) longJsonVar).get("verylongjson").toString().length() >= 4000) {
                results.put("test_long_json_variable_result", "able to read long json");
            }
        }

        Object intVar = event.getIntegrationContext().getInBoundVariables().get("test_int_variable_name");
        if (intVar != null && intVar instanceof Integer) {
            results.put("test_int_variable_result", "able to read integer");
        }

        Object boolVar = event.getIntegrationContext().getInBoundVariables().get("test_bool_variable_name");
        if (boolVar != null && boolVar instanceof Boolean) {
            results.put("test_bool_variable_result", "able to read boolean");
        }

        Object bigDecimalVar = event.getIntegrationContext().getInBoundVariable("test_bigdecimal_variable_name");
        logger.info("bigDecimalVar value as string " + bigDecimalVar);
        if (
            bigDecimalVar != null &&
            bigDecimalVar instanceof BigDecimal &&
            BigDecimal.valueOf(1234567890L, 2).equals(bigDecimalVar)
        ) {
            results.put("test_bigdecimal_variable_result", bigDecimalVar);
        }

        Object longVar = event.getIntegrationContext().getInBoundVariable("test_long_variable_name");
        logger.info("longVar value as string " + longVar);
        if (longVar != null && longVar instanceof Long && Long.valueOf(1234567890L).equals(longVar)) {
            results.put("test_long_variable_result", longVar);
        }

        Object dateVar = event.getIntegrationContext().getInBoundVariable("test_date_variable_name");
        logger.info("dateVar value as string " + dateVar);
        if (dateVar != null && dateVar instanceof Date) {
            results.put("test_date_variable_result", dateVar);
        }

        results.put("var1", var1);
        Message<IntegrationResult> message = IntegrationResultBuilder
            .resultFor(event, connectorProperties)
            .withOutboundVariables(results)
            .buildMessage();
        integrationResultSender.send(message);
    }

    public String getVar1Copy() {
        return var1Copy;
    }
}
