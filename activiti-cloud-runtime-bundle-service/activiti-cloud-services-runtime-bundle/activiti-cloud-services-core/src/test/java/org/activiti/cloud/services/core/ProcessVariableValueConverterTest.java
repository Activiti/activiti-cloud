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
    private ProcessVariableValueConverter processVariableValueConverter;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    @Test
    void testProcessVariableValueConverterNullValue() {
        // when
        String nullValue = processVariableValueConverter.convert(new ProcessVariableValue("String", null));

        // then
        assertThat(nullValue).isNull();
    }

    @Test
    void testProcessVariableValueConverterStringValue() {
        // when
        String stringValue = processVariableValueConverter.convert(new ProcessVariableValue("string", "name"));

        // then
        assertThat(stringValue).isEqualTo("name");
    }

    @Test
    void testProcessVariableValueConverterIntValue() {
        // when
        Integer intValue = processVariableValueConverter.convert(new ProcessVariableValue("int", "10"));

        // then
        assertThat(intValue).isEqualTo(10);
    }

    @Test
    void testProcessVariableValueConverterLongValue() {
        // when
        Long longValue = processVariableValueConverter.convert(new ProcessVariableValue("long", "10"));

        // then
        assertThat(longValue).isEqualTo(10L);
    }

    @Test
    void testProcessVariableValueConverterBooleanValue() {
        // when
        Boolean booleanValue = processVariableValueConverter.convert(new ProcessVariableValue("boolean", "true"));

        // then
        assertThat(booleanValue).isTrue();
    }

    @Test
    void testProcessVariableValueConverterDoubleValue() {
        // when
        Double doubleValue = processVariableValueConverter.convert(new ProcessVariableValue("double", "10.00"));

        // then
        assertThat(doubleValue).isEqualTo(10.00);
    }

    @Test
    void testProcessVariableValueConverterLocalDateValue() {
        // when
        LocalDate localDateValue = processVariableValueConverter.convert(
            new ProcessVariableValue("LocalDate", "2020-04-20")
        );

        // then
        assertThat(localDateValue).isEqualTo(LocalDate.of(2020, 4, 20));
    }

    @Test
    void testProcessVariableValueConverterDateValue() {
        // when
        Date dateValue = processVariableValueConverter.convert(
            new ProcessVariableValue("Date", DATE_1970_01_01T01_01_01_001Z)
        );

        // then
        assertThat(dateValue).isEqualTo(dateFormatterProvider.parse(DATE_1970_01_01T01_01_01_001Z));
    }

    @Test
    void testProcessVariableValueConverterBigDecimalValue() {
        // when
        BigDecimal bigDecimalValue = processVariableValueConverter.convert(
            new ProcessVariableValue("BigDecimal", "10.00")
        );

        // then
        assertThat(bigDecimalValue).isEqualTo(BigDecimal.valueOf(1000, 2));
    }

    @Test
    void testProcessVariableValueConverterJsonNodeValue() {
        // when
        JsonNode jsonNodeValue = processVariableValueConverter.convert(new ProcessVariableValue("json", "{}"));

        // then
        assertThat(jsonNodeValue).isEqualTo(JsonNodeFactory.instance.objectNode());
    }
}
