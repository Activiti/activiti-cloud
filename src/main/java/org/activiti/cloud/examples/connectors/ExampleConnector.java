package org.activiti.cloud.examples.connectors;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static net.logstash.logback.marker.Markers.append;

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

        logger.info(append("service-name",
                           appName),
                    ">>> In example-cloud-connector");

        String var1 = ExampleConnector.class.getSimpleName()+" was called for instance " + event.getIntegrationContext().getProcessInstanceId();

        var1Copy = String.valueOf(var1);

        Object jsonVar = event.getIntegrationContext().getInBoundVariables().get("test-json-variable-name");
        Object longJsonVar = event.getIntegrationContext().getInBoundVariables().get("test-long-json-variable-name");

        Map<String, Object> results = new HashMap<>();

        if(jsonVar != null){
            logger.info("jsonVar value type "+jsonVar.getClass().getTypeName());
            logger.info("jsonVar value as string "+jsonVar.toString());

            CustomPojo customPojo = objectMapper.convertValue(jsonVar,CustomPojo.class);
            results.put("test-json-variable-result","able to convert test-json-variable-name to "+CustomPojo.class.getName());
        }


        if( longJsonVar != null && longJsonVar instanceof LinkedHashMap){
            if(((LinkedHashMap) longJsonVar).get("verylongjson").toString().length() >= 4000){
                results.put("test-long-json-variable-result","able to read long json");
            }

        }

        Object intVar = event.getIntegrationContext().getInBoundVariables().get("test-int-variable-name");
        if( intVar != null && intVar instanceof Integer){
            results.put("test-int-variable-result","able to read integer");
        }

        Object boolVar = event.getIntegrationContext().getInBoundVariables().get("test-bool-variable-name");
        if( boolVar != null && boolVar instanceof Boolean){
            results.put("test-bool-variable-result","able to read boolean");
        }

        results.put("var1",
                    var1);
        Message<IntegrationResult> message = IntegrationResultBuilder.resultFor(event, connectorProperties)
                .withOutboundVariables(results)
                .buildMessage();
        integrationResultSender.send(message);
    }

    public String getVar1Copy() {
        return var1Copy;
    }

}
