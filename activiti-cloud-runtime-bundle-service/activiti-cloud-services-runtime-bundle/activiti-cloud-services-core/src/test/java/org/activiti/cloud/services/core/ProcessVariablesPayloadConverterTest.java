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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.cloud.identity.IdentityService;
import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.activiti.common.util.DateFormatterProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApplication.class)
@TestPropertySource("classpath:application-test.properties")
class ProcessVariablesPayloadConverterTest {

    private static final String DATE_1970_01_01T01_01_01_001Z = "1970-01-01T01:01:01.001Z";

    @Autowired
    private ProcessVariablesPayloadConverter subject;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    @MockBean
    private IdentityService identityService;

    private static Map<String, Object> variablesToConvert;

    @BeforeAll
    static void buildVariablesMap() {
        variablesToConvert = new HashMap<>();
        variablesToConvert.put("int", 123);
        variablesToConvert.put("string", "a string");
        variablesToConvert.put("boolean", true);
        variablesToConvert.put("nullValue", new ProcessVariableValue("String", null).toMap());
        variablesToConvert.put("stringValue", new ProcessVariableValue("string", "name").toMap());
        variablesToConvert.put("quoteValue", new ProcessVariableValue("string", "\"").toMap());
        variablesToConvert.put("intValue", new ProcessVariableValue("int", "10").toMap());
        variablesToConvert.put("longValue", new ProcessVariableValue("long", "10").toMap());
        variablesToConvert.put("booleanValue", new ProcessVariableValue("boolean", "true").toMap());
        variablesToConvert.put("doubleValue", new ProcessVariableValue("double", "10.00").toMap());
        variablesToConvert.put("localDateValue", new ProcessVariableValue("localdate", "2020-04-20").toMap());
        variablesToConvert.put("dateValue", new ProcessVariableValue("date", DATE_1970_01_01T01_01_01_001Z).toMap());
        variablesToConvert.put("bigDecimalValue", new ProcessVariableValue("BigDecimal", "10.00").toMap());
        variablesToConvert.put("jsonNodeValue", new ProcessVariableValue("json", "{}").toMap());
        variablesToConvert.put("jsonNodeValue2", new ProcessVariableValue("json", "{}"));
        variablesToConvert.put("integerAmount", new ProcessVariableValue("amount", 20));
        variablesToConvert.put("doubleAmount", new ProcessVariableValue("amount", 30.2));
    }

    private void assertConvertedVariables(Map<String, Object> convertedVariables) {
        assertThat(convertedVariables)
            .containsEntry("int", 123)
            .containsEntry("string", "a string")
            .containsEntry("boolean", true)
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
            .containsEntry("jsonNodeValue2", JsonNodeFactory.instance.objectNode())
            .containsEntry("integerAmount", 20)
            .containsEntry("doubleAmount", 30.2);
    }

    @Test
    void shouldConvertStartProcessPayload() {
        StartProcessPayload payload = subject.convert(
            ProcessPayloadBuilder.start().withVariables(variablesToConvert).build()
        );
        assertConvertedVariables(payload.getVariables());
    }

    @Test
    void shouldConvertCompleteTaskPayload() {
        CompleteTaskPayload result = subject.convert(
            TaskPayloadBuilder.complete().withVariables(variablesToConvert).build()
        );
        assertConvertedVariables(result.getVariables());
    }

    @Test
    void shouldConvertSaveTaskPayload() {
        SaveTaskPayload result = subject.convert(TaskPayloadBuilder.save().withVariables(variablesToConvert).build());
        assertConvertedVariables(result.getVariables());
    }
}
