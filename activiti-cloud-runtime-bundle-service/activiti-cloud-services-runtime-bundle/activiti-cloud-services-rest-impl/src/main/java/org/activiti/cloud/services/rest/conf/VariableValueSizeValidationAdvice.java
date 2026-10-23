/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.rest.conf;

import java.lang.reflect.Type;
import java.util.Set;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * A {@link org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice} that
 * validates the combined size of non-whitelisted variable values after Jackson deserialization.
 * <p>
 * This complements {@link VariableRequestSizeLimitFilter} (which enforces total request body
 * size at the servlet level) by adding per-variable content inspection at the application level.
 */
@ControllerAdvice
public class VariableValueSizeValidationAdvice extends RequestBodyAdviceAdapter {

    private static final Set<Class<?>> SUPPORTED_PAYLOAD_TYPES = Set.of(
        SetProcessVariablesPayload.class,
        StartProcessPayload.class,
        CreateTaskVariablePayload.class,
        UpdateTaskVariablePayload.class
    );

    private final VariableValueSizeValidator validator;

    public VariableValueSizeValidationAdvice(VariableValueSizeValidator validator) {
        this.validator = validator;
    }

    @Override
    public boolean supports(
        MethodParameter methodParameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (targetType instanceof Class<?> clazz) {
            return SUPPORTED_PAYLOAD_TYPES.stream().anyMatch(supported -> supported.isAssignableFrom(clazz));
        }
        return false;
    }

    @Override
    public Object afterBodyRead(
        Object body,
        HttpInputMessage inputMessage,
        MethodParameter parameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (body instanceof SetProcessVariablesPayload payload) {
            validator.validate(payload.getVariables());
        } else if (body instanceof StartProcessPayload payload) {
            validator.validate(payload.getVariables());
        } else if (body instanceof CreateTaskVariablePayload payload) {
            validator.validateSingle(payload.getName(), payload.getValue());
        } else if (body instanceof UpdateTaskVariablePayload payload) {
            validator.validateSingle(payload.getName(), payload.getValue());
        }
        return body;
    }
}
