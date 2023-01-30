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
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@ConnectorBinding(input = ExampleConnectorChannels.EXAMPLE_CONNECTOR_CONSUMER, condition = "")
@Component(ExampleConnectorChannels.EXAMPLE_CONNECTOR_CONSUMER + "Connector")
public class ExampleConnector implements Connector<IntegrationRequest, Void> {

    private final Logger logger = LoggerFactory.getLogger(ExampleConnector.class);

    @Value("${spring.application.name}")
    private String appName;

    //just a convenience - not recommended in real implementations
    private String var1Copy = "";

    private final ConnectorProperties connectorProperties;

    private final ObjectMapper objectMapper;

    private final IntegrationResultSender integrationResultSender;

    @Autowired
    public ExampleConnector(
        ConnectorProperties connectorProperties,
        IntegrationResultSender integrationResultSender,
        ObjectMapper objectMapper
    ) {
        this.connectorProperties = connectorProperties;
        this.objectMapper = objectMapper;
        this.integrationResultSender = integrationResultSender;
    }

    @Override
    public Void apply(IntegrationRequest event) {
        logger.info(append("service-name", appName), ">>> In example-cloud-connector");

        String var1 =
            ExampleConnector.class.getSimpleName() +
            " was called for instance " +
            event.getIntegrationContext().getProcessInstanceId();

        var1Copy = var1;

        Object jsonVar = event.getIntegrationContext().getInBoundVariables().get("test_json_variable_name");
        Object longJsonVar = event.getIntegrationContext().getInBoundVariables().get("test_long_json_variable_name");

        Map<String, Object> results = new HashMap<>();

        processJsonVar(jsonVar, results);

        processLongJsonVar(longJsonVar, results);

        Object intVar = event.getIntegrationContext().getInBoundVariables().get("test_int_variable_name");
        processIntVar(results, intVar);

        Object boolVar = event.getIntegrationContext().getInBoundVariables().get("test_bool_variable_name");
        processBoolVar(results, boolVar);

        Object bigDecimalVar = event.getIntegrationContext().getInBoundVariable("test_bigdecimal_variable_name");
        processBigDecimalVar(results, bigDecimalVar);

        Object longVar = event.getIntegrationContext().getInBoundVariable("test_long_variable_name");
        processLongVar(results, longVar);

        Object dateVar = event.getIntegrationContext().getInBoundVariable("test_date_variable_name");
        processDateVar(results, dateVar);

        results.put("var1", var1);
        Message<IntegrationResult> message = IntegrationResultBuilder
            .resultFor(event, connectorProperties)
            .withOutboundVariables(results)
            .buildMessage();

        integrationResultSender.send(message);
        return null;
    }

    private void processJsonVar(Object jsonVar, Map<String, Object> results) {
        if (jsonVar != null) {
            logger.info("jsonVar value type " + jsonVar.getClass().getTypeName());
            logger.info("jsonVar value as string " + jsonVar.toString());

            CustomPojo customPojo = objectMapper.convertValue(jsonVar, CustomPojo.class);
            results.put(
                "test_json_variable_result",
                "able to convert test_json_variable_name to " + CustomPojo.class.getName()
            );
        }
    }

    private void processLongJsonVar(Object longJsonVar, Map<String, Object> results) {
        if (
            longJsonVar instanceof LinkedHashMap &&
            ((LinkedHashMap<?, ?>) longJsonVar).get("verylongjson").toString().length() >= 4000
        ) {
            results.put("test_long_json_variable_result", "able to read long json");
        }
    }

    private void processIntVar(Map<String, Object> results, Object intVar) {
        if (intVar instanceof Integer) {
            results.put("test_int_variable_result", "able to read integer");
        }
    }

    private void processBoolVar(Map<String, Object> results, Object boolVar) {
        if (boolVar instanceof Boolean) {
            results.put("test_bool_variable_result", "able to read boolean");
        }
    }

    private void processBigDecimalVar(Map<String, Object> results, Object bigDecimalVar) {
        logger.info("bigDecimalVar value as string " + bigDecimalVar);
        if (bigDecimalVar instanceof BigDecimal && BigDecimal.valueOf(1234567890L, 2).equals(bigDecimalVar)) {
            results.put("test_bigdecimal_variable_result", bigDecimalVar);
        }
    }

    private void processLongVar(Map<String, Object> results, Object longVar) {
        logger.info("longVar value as string " + longVar);
        if (longVar instanceof Long && Long.valueOf(1234567890L).equals(longVar)) {
            results.put("test_long_variable_result", longVar);
        }
    }

    private void processDateVar(Map<String, Object> results, Object dateVar) {
        logger.info("dateVar value as string " + dateVar);
        if (dateVar instanceof Date) {
            results.put("test_date_variable_result", dateVar);
        }
    }

    public String getVar1Copy() {
        return var1Copy;
    }
}
