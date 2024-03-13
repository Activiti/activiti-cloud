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
package org.activiti.cloud.services.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.stream.Stream;
import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.activiti.common.util.DateFormatterProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApplication.class)
@TestPropertySource("classpath:application-test.properties")
class ProcessVariableValueConverterTest {

    private static final String DATE_1970_01_01T01_01_01_001Z = "1970-01-01T01:01:01.001Z";

    @Autowired
    private ProcessVariableValueConverter variableValueConverter;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    @Test
    void testProcessVariableValueConverterNullValue() {
        // when
        String nullValue = variableValueConverter.convert(new ProcessVariableValue("String", null));

        // then
        assertThat(nullValue).isNull();
    }

    @Test
    void testProcessVariableValueConverterStringValue() {
        // when
        String stringValue = variableValueConverter.convert(new ProcessVariableValue("string", "name"));

        // then
        assertThat(stringValue).isEqualTo("name");
    }

    @Test
    void testProcessVariableValueConverterIntegerValue() {
        assertThat(
            Stream
                .of("10", 10, 10.0, 10L, 10f)
                .map(value -> new ProcessVariableValue("int", value))
                .map(variableValueConverter::convert)
        )
            .allSatisfy(convertedValue -> assertThat(convertedValue).isEqualTo(10));

        assertThat(
            Stream
                .of("10", 10, 10.0, 10L, 10f)
                .map(value -> new ProcessVariableValue("integer", value))
                .map(variableValueConverter::convert)
        )
            .allSatisfy(convertedValue -> assertThat(convertedValue).isEqualTo(10));
    }

    @Test
    void testProcessVariableValueConverterLongValue() {
        assertThat(
            Stream
                .of("10", 10, 10.0, 10L, 10f)
                .map(value -> new ProcessVariableValue("long", value))
                .map(variableValueConverter::convert)
        )
            .allSatisfy(convertedValue -> assertThat(convertedValue).isEqualTo(10L));
    }

    @Test
    void testProcessVariableValueConverterBooleanValue() {
        assertThat(
            Stream
                .of(true, "true")
                .map(value -> new ProcessVariableValue("boolean", value))
                .map(variableValueConverter::convert)
        )
            .allSatisfy(convertedValue -> assertThat(convertedValue).isEqualTo(true));
    }

    @Test
    void testProcessVariableValueConverterDoubleValue() {
        assertThat(
            Stream
                .of("10", 10, 10.0, 10L, 10f)
                .map(value -> new ProcessVariableValue("double", value))
                .map(variableValueConverter::convert)
        )
            .allSatisfy(convertedValue -> assertThat(convertedValue).isEqualTo(10.0));
    }

    @Test
    void testProcessVariableValueConverterLocalDateValue() {
        // when
        LocalDate localDateValue = variableValueConverter.convert(new ProcessVariableValue("LocalDate", "2020-04-20"));

        // then
        assertThat(localDateValue).isEqualTo(LocalDate.of(2020, 4, 20));
    }

    @Test
    void testProcessVariableValueConverterDateValue() {
        // when
        Date dateValue = variableValueConverter.convert(
            new ProcessVariableValue("Date", DATE_1970_01_01T01_01_01_001Z)
        );

        // then
        assertThat(dateValue).isEqualTo(dateFormatterProvider.parse(DATE_1970_01_01T01_01_01_001Z));
    }

    @Test
    void testProcessVariableValueConverterBigDecimalValue() {
        assertThat(
            Stream
                .of("10", 10, 10.0, 10L, 10f)
                .map(value -> new ProcessVariableValue("bigdecimal", value))
                .map(variableValueConverter::convert)
        )
            .allSatisfy(convertedValue ->
                assertThat((BigDecimal) convertedValue).isEqualByComparingTo(BigDecimal.valueOf(10))
            );
    }

    @Test
    void testProcessVariableValueConverterJsonNodeValue() {
        // when
        JsonNode jsonNodeValue = variableValueConverter.convert(new ProcessVariableValue("json", "{}"));

        // then
        assertThat(jsonNodeValue).isEqualTo(JsonNodeFactory.instance.objectNode());
    }

    @Test
    void testProcessVariableValueConverterTypeNotPresentInRegistry() {
        // when
        Integer amountValue = 20;
        Object converted = variableValueConverter.convert(new ProcessVariableValue("amount", amountValue));
        // then
        assertThat(amountValue).isEqualTo(converted);
    }
}
