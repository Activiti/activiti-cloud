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

package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.context.request.NativeWebRequest;

@ExtendWith(MockitoExtension.class)
public class VariableSearchArgumentResolverTest {

    private static final String VARIABLES_NAME_KEY = "variables.name";
    private static final String VARIABLES_VALUE_KEY = "variables.value";
    private static final String VARIABLES_TYPE_KEY = "variables.type";

    @InjectMocks
    private VariableSearchArgumentResolver argumentResolver;

    @Mock
    private ConversionService conversionService;

    @Test
    public void supportsParameter_should_returnTrue_when_itsVariableSearch() {
        //given
        MethodParameter methodParameter = buildMethodParameter(VariableSearch.class);

        //when
        boolean supportsParameter = argumentResolver.supportsParameter(methodParameter);

        //then
        assertThat(supportsParameter).isTrue();
    }

    @Test
    public void supportsParameter_should_returnFalse_when_itsNotVariableSearch() {
        //given
        MethodParameter methodParameter = buildMethodParameter(String.class);

        //when
        boolean supportsParameter = argumentResolver.supportsParameter(methodParameter);

        //then
        assertThat(supportsParameter).isFalse();
    }

    private MethodParameter buildMethodParameter(Class<?> type) {
        MethodParameter methodParameter = mock(MethodParameter.class);
        doReturn(type).when(methodParameter).getParameterType();
        return methodParameter;
    }

    @Test
    public void resolveArgument_should_constructVariableSearchFromQueryParametersAfterConvertingValue() {
        //given
        String variableName = "myVar";
        String variableValue = "10";
        String variableType = "integer";
        NativeWebRequest webRequest = buildWebRequest(variableName, variableValue, variableType);
        given(conversionService.convert(variableValue, Integer.class)).willReturn(10);

        //when
        Object resolvedArgument = argumentResolver.resolveArgument(mock(MethodParameter.class), null, webRequest, null);

        //then
        assertThat(resolvedArgument).isInstanceOf(VariableSearch.class);
        VariableSearch variableSearch = (VariableSearch) resolvedArgument;
        assertThat(variableSearch.getName()).isEqualTo(variableName);
        assertThat(variableSearch.getValue().getValue()).isEqualTo(10);
        assertThat(variableSearch.getType()).isEqualTo(variableType);
    }

    @Test
    public void resolveArgument_should_useRawValueWithoutConversion_when_typeIsString() {
        //given
        String variableName = "myVar";
        String variableValue = "text";
        String variableType = "string";
        NativeWebRequest webRequest = buildWebRequest(variableName, variableValue, variableType);

        //when
        Object resolvedArgument = argumentResolver.resolveArgument(mock(MethodParameter.class), null, webRequest, null);

        //then
        assertThat(resolvedArgument).isInstanceOf(VariableSearch.class);
        VariableSearch variableSearch = (VariableSearch) resolvedArgument;
        assertThat(variableSearch.getName()).isEqualTo(variableName);
        assertThat(variableSearch.getValue().getValue()).isEqualTo(variableValue);
        assertThat(variableSearch.getType()).isEqualTo(variableType);

        verifyNoInteractions(conversionService);
    }

    private NativeWebRequest buildWebRequest(String variableName, String variableValue, String variableType) {
        NativeWebRequest webRequest = buildWebRequest(variableName, variableValue);
        given(webRequest.getParameter(VARIABLES_TYPE_KEY)).willReturn(variableType);
        return webRequest;
    }

    private NativeWebRequest buildWebRequest(String variableName, String variableValue) {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        given(webRequest.getParameter(VARIABLES_NAME_KEY)).willReturn(variableName);
        given(webRequest.getParameter(VARIABLES_VALUE_KEY)).willReturn(variableValue);
        return webRequest;
    }
}
