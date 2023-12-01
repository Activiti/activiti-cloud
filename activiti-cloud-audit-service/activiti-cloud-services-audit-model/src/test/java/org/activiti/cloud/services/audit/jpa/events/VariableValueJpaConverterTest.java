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
package org.activiti.cloud.services.audit.jpa.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import org.activiti.cloud.services.audit.api.AuditException;
import org.junit.jupiter.api.Test;

class VariableValueJpaConverterTest {

    private VariableValueJpaConverter variableValueJpaConverter = new VariableValueJpaConverter();

    @Test
    void should_convertToDatabaseColumn_when_stringVariableValue() {
        VariableValue<String> variableValue = new VariableValue<>();
        variableValue.setValue("stringValue");

        String result = variableValueJpaConverter.convertToDatabaseColumn(variableValue);

        assertThat(result).isEqualTo("{\"value\":\"stringValue\"}");
    }

    @Test
    void should_convertToDatabaseColumn_when_integerVariableValue() {
        VariableValue<Integer> variableValue = new VariableValue<>();
        variableValue.setValue(12);

        String result = variableValueJpaConverter.convertToDatabaseColumn(variableValue);

        assertThat(result).isEqualTo("{\"value\":12}");
    }

    @Test
    void should_convertToDatabaseColumn_when_booleanVariableValue() {
        VariableValue<Boolean> variableValue = new VariableValue<>();
        variableValue.setValue(true);

        String result = variableValueJpaConverter.convertToDatabaseColumn(variableValue);

        assertThat(result).isEqualTo("{\"value\":true}");
    }

    @Test
    void should_convertToDatabaseColumn_when_doubleVariableValue() {
        VariableValue<Double> variableValue = new VariableValue<>();
        variableValue.setValue(12.34);

        String result = variableValueJpaConverter.convertToDatabaseColumn(variableValue);

        assertThat(result).isEqualTo("{\"value\":12.34}");
    }

    @Test
    void should_convertToDatabaseColumn_when_bigDecimalVariableValue() {
        VariableValue<BigDecimal> variableValue = new VariableValue<>();
        variableValue.setValue(BigDecimal.valueOf(1234.5678));

        String result = variableValueJpaConverter.convertToDatabaseColumn(variableValue);

        assertThat(result).isEqualTo("{\"value\":1234.5678}");
    }

    @Test
    void should_convertToDatabaseColumn_when_nullValue() {
        String result = variableValueJpaConverter.convertToDatabaseColumn(null);

        assertThat(result).isEqualTo("null");
    }

    @Test
    void should_convertToEntityAttribute_when_stringEntity() {
        VariableValue<?> result = variableValueJpaConverter.convertToEntityAttribute("{\"value\":\"stringValue\"}");

        assertThat(result.getValue()).isEqualTo("stringValue");
    }

    @Test
    void should_convertToEntityAttribute_when_booleanEntity() {
        VariableValue<?> result = variableValueJpaConverter.convertToEntityAttribute("{\"value\":true}");

        assertThat(result.getValue()).isEqualTo(true);
    }

    @Test
    void should_convertToEntityAttribute_when_integerEntity() {
        VariableValue<?> result = variableValueJpaConverter.convertToEntityAttribute("{\"value\":123}");

        assertThat(result.getValue()).isEqualTo(123);
    }

    @Test
    void should_convertToEntityAttribute_when_doubleEntity() {
        VariableValue<?> result = variableValueJpaConverter.convertToEntityAttribute("{\"value\":1234.5678}");

        assertThat(result.getValue()).isEqualTo(1234.5678);
    }

    @Test
    void should_convertToEntityAttribute_when_entityNull() {
        VariableValue<?> result = variableValueJpaConverter.convertToEntityAttribute(null);

        assertThat(result).isNull();
    }

    @Test
    void should_convertToEntityAttribute_when_entityEmpty() {
        VariableValue<?> result = variableValueJpaConverter.convertToEntityAttribute("");

        assertThat(result).isNull();
    }

    @Test
    void should_throwExceptionConvertToEntityAttribute() {
        Throwable exception = catchThrowable(() -> variableValueJpaConverter.convertToEntityAttribute("{..invalidJson")
        );

        assertThat(exception).isInstanceOf(AuditException.class).hasMessage("Unable to deserialize object.");
    }
}
