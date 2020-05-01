package org.activiti.cloud.services.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.activiti.cloud.services.core.utils.TestProcessEngineConfiguration;
import org.activiti.common.util.DateFormatterProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestProcessEngineConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
@Disabled("No bean named 'commandConsumer' available")
public class ProcessVariablesPayloadConverterTest {

    private static final String DATE_1970_01_01T01_01_01_001Z = "1970-01-01T01:01:01.001Z";

    @Autowired
    private ProcessVariablesPayloadConverter subject;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    @Test
    public void testProcessVariablesPayloadConverter() {
        // given
        Map<String, Object> input = new HashMap<>();

        input.put("int", 123);
        input.put("string", "123");
        input.put("bool", true);
        input.put("nullValue", new ProcessVariableValue("String", null).toMap());
        input.put("stringValue", new ProcessVariableValue("string", "name").toMap());
        input.put("quoteValue", new ProcessVariableValue("string", "\"").toMap());
        input.put("intValue", new ProcessVariableValue("int", "10").toMap());
        input.put("longValue", new ProcessVariableValue("long", "10").toMap());
        input.put("booleanValue", new ProcessVariableValue("boolean", "true").toMap());
        input.put("doubleValue", new ProcessVariableValue("double", "10.00").toMap());
        input.put("localDateValue", new ProcessVariableValue("localdate", "2020-04-20").toMap());
        input.put("dateValue", new ProcessVariableValue("date", DATE_1970_01_01T01_01_01_001Z).toMap());
        input.put("bigDecimalValue", new ProcessVariableValue("BigDecimal", "10.00").toMap());
        input.put("jsonNodeValue", new ProcessVariableValue("json", "{}").toMap());
        input.put("jsonNodeValue2", new ProcessVariableValue("json", "{}"));

        // when
        StartProcessPayload result = subject.convert(ProcessPayloadBuilder.start()
                                                                          .withVariables(input)
                                                                          .build());
        // then
        assertThat(result.getVariables()).containsEntry("int", 123)
                          .containsEntry("string", "123")
                          .containsEntry("bool", true)
                          .containsEntry("nullValue", null)
                          .containsEntry("stringValue", "name")
                          .containsEntry("quoteValue", "\"")
                          .containsEntry("intValue", 10)
                          .containsEntry("longValue", 10L)
                          .containsEntry("booleanValue", true)
                          .containsEntry("doubleValue", 10.00)
                          .containsEntry("localDateValue", LocalDate.of(2020, 4, 20))
                          .containsEntry("dateValue", dateFormatterProvider.parse(DATE_1970_01_01T01_01_01_001Z))
                          .containsEntry("bigDecimalValue", BigDecimal.valueOf(1000, 2))
                          .containsEntry("jsonNodeValue", JsonNodeFactory.instance.objectNode())
                          .containsEntry("jsonNodeValue2", JsonNodeFactory.instance.objectNode());
    }

}
