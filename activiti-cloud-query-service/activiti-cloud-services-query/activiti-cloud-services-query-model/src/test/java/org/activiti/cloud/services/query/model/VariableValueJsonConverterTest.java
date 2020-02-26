/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class VariableValueJsonConverterTest {

    @InjectMocks
    private VariableValueJsonConverter converter;

    @Mock
    private ObjectMapper objectMapper;

    private static final VariableValue<Integer> ENTITY_REPRESENTATION = new VariableValue<>(10);
    private static final String JSON_REPRESENTATION = "{\"value\": 10}";

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void convertToDatabaseColumnShouldConvertToJson() throws Exception {
        //given
        given(objectMapper.writeValueAsString(ENTITY_REPRESENTATION)).willReturn(JSON_REPRESENTATION);

        //when
        String convertedValue = converter.convertToDatabaseColumn(ENTITY_REPRESENTATION);

        //then
        assertThat(convertedValue).isEqualTo(JSON_REPRESENTATION);
    }

    @Test
    public void convertToDatabaseColumnShouldThrowQueryExceptionWhenAnExceptionOccursWhileProcessing() throws Exception {
        //given
        MockJsonProcessingException exception = new MockJsonProcessingException("any");
        given(objectMapper.writeValueAsString(ENTITY_REPRESENTATION)).willThrow(exception);

        //when
        Throwable thrown = catchThrowable(() ->
                                                  converter.convertToDatabaseColumn(ENTITY_REPRESENTATION));

        //then
        assertThat(thrown)
                .isInstanceOf(QueryException.class)
                .hasMessage("Unable to serialize variable.")
                .hasCause(exception);
    }

    @Test
    public void convertToEntityAttributeShouldConvertFromJson() throws Exception {
        //given
        given(objectMapper.readValue(JSON_REPRESENTATION,
                                     VariableValue.class)).willReturn(ENTITY_REPRESENTATION);

        //when
        VariableValue<?> convertedValue = converter.convertToEntityAttribute(JSON_REPRESENTATION);

        //then
        assertThat(convertedValue).isEqualTo(ENTITY_REPRESENTATION);
    }

    @Test
    public void convertToEntityAttributeShouldThrowExceptionWhenExceptionOccursWhileReading() throws Exception {
        //given
        IOException ioException = new IOException();
        given(objectMapper.readValue(JSON_REPRESENTATION,
                                     VariableValue.class)).willThrow(ioException);

        //when
        Throwable thrown = catchThrowable(() -> converter.convertToEntityAttribute(JSON_REPRESENTATION));

        //then
        assertThat(thrown)
        .isInstanceOf(QueryException.class)
        .hasMessage("Unable to deserialize variable.")
        .hasCause(ioException);
    }
}