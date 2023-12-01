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

import org.activiti.api.runtime.model.impl.ProcessVariablesMapTypeRegistry;
import org.activiti.cloud.services.query.model.VariableValue;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class VariableSearchArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String VARIABLE_NAME_KEY = "variables.name";
    private static final String VARIABLE_VALUE_KEY = "variables.value";
    private static final String VARIABLE_TYPE_KEY = "variables.type";

    private ConversionService conversionService;

    public VariableSearchArgumentResolver(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(VariableSearch.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        String variableType = webRequest.getParameter(VARIABLE_TYPE_KEY);
        Class<?> type = ProcessVariablesMapTypeRegistry.forType(variableType, String.class);

        Object variableValueParameter = webRequest.getParameter(VARIABLE_VALUE_KEY);
        if (!String.class.equals(type)) {
            variableValueParameter = conversionService.convert(variableValueParameter, type);
        }

        String variableName = webRequest.getParameter(VARIABLE_NAME_KEY);
        return new VariableSearch(variableName, new VariableValue<>(variableValueParameter), variableType);
    }
}
