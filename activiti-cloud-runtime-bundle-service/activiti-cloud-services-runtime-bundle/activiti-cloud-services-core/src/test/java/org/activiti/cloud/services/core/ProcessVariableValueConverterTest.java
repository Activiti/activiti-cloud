package org.activiti.cloud.services.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.activiti.cloud.services.core.utils.TestProcessEngineConfiguration;
import org.activiti.common.util.DateFormatterProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestProcessEngineConfiguration.class)
@TestPropertySource("classpath:test-application.properties")
public class ProcessVariableValueConverterTest {

    private static final String DATE_1970_01_01T01_01_01_001Z = "1970-01-01T01:01:01.001Z";

    @Autowired
    private ProcessVariableValueConverter variableValueConverter;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    @Test
    public void testProcessVariableValueConverterNullValue() {
        // when
        String nullValue = variableValueConverter.convert(new ProcessVariableValue("String", null));

        // then
        assertThat(nullValue).isNull();
    }

    @Test
    public void testProcessVariableValueConverterStringValue() {
        // when
        String stringValue = variableValueConverter.convert(new ProcessVariableValue("string", "name"));

        // then
        assertThat(stringValue).isEqualTo("name");
    }

    @Test
    public void testProcessVariableValueConverterIntValue() {
        // when
        Integer intValue = variableValueConverter.convert(new ProcessVariableValue("int", "10"));

        // then
        assertThat(intValue).isEqualTo(10);
    }

    @Test
    public void testProcessVariableValueConverterLongValue() {
        // when
        Long longValue = variableValueConverter.convert(new ProcessVariableValue("long", "10"));

        // then
        assertThat(longValue).isEqualTo(10L);
    }

    @Test
    public void testProcessVariableValueConverterBooleanValue() {
        // when
        Boolean booleanValue = variableValueConverter.convert(new ProcessVariableValue("boolean", "true"));

        // then
        assertThat(booleanValue).isEqualTo(true);
    }

    @Test
    public void testProcessVariableValueConverterDoubleValue() {
        // when
        Double doubleValue = variableValueConverter.convert(new ProcessVariableValue("double", "10.00"));

        // then
        assertThat(doubleValue).isEqualTo(10.00);
    }

    @Test
    public void testProcessVariableValueConverterLocalDateValue() {
        // when
        LocalDate localDateValue = variableValueConverter.convert(new ProcessVariableValue("LocalDate", "2020-04-20"));

        // then
        assertThat(localDateValue).isEqualTo(LocalDate.of(2020, 4, 20));
    }

    @Test
    public void testProcessVariableValueConverterDateValue() {
        // when
        Date dateValue = variableValueConverter.convert(new ProcessVariableValue("Date", DATE_1970_01_01T01_01_01_001Z));

        // then
        assertThat(dateValue).isEqualTo(dateFormatterProvider.parse(DATE_1970_01_01T01_01_01_001Z));
    }

    @Test
    public void testProcessVariableValueConverterBigDecimalValue() {
        // when
        BigDecimal bigDecimalValue = variableValueConverter.convert(new ProcessVariableValue("BigDecimal", "10.00"));

        // then
        assertThat(bigDecimalValue).isEqualTo(BigDecimal.valueOf(1000, 2));
    }

    @Test
    public void testProcessVariableValueConverterJsonNodeValue() {
        // when
        JsonNode jsonNodeValue = variableValueConverter.convert(new ProcessVariableValue("json", "{}"));

        // then
        assertThat(jsonNodeValue).isEqualTo(JsonNodeFactory.instance.objectNode());
    }

}
