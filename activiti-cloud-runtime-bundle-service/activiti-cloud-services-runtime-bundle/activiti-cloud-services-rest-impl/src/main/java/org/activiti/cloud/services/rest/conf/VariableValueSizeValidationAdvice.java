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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.databind.ObjectMapper;

@ControllerAdvice
@EnableConfigurationProperties(VariableSizeLimitProperties.class)
public class VariableValueSizeValidationAdvice extends RequestBodyAdviceAdapter {

    private static final Map<Class<?>, Function<Object, Map<String, Object>>> VARIABLE_EXTRACTORS = Map.of(
        SetProcessVariablesPayload.class,
        payload -> ((SetProcessVariablesPayload) payload).getVariables(),
        StartProcessPayload.class,
        payload -> ((StartProcessPayload) payload).getVariables(),
        CreateTaskVariablePayload.class,
        payload -> {
            CreateTaskVariablePayload createPayload = (CreateTaskVariablePayload) payload;
            return Collections.singletonMap(createPayload.getName(), createPayload.getValue());
        },
        UpdateTaskVariablePayload.class,
        payload -> {
            // The controller overwrites payload.name with the URI's {variableName} AFTER
            // this advice runs, so we must validate against the URI variable name to prevent
            // bypass (body claims a whitelisted name while the URI targets a non-whitelisted one).
            UpdateTaskVariablePayload updatePayload = (UpdateTaskVariablePayload) payload;
            String resolvedName = extractVariableNameFromUri();
            return Collections.singletonMap(
                resolvedName != null ? resolvedName : updatePayload.getName(),
                updatePayload.getValue()
            );
        }
    );

    private final VariableValueSizeValidator validator;

    public VariableValueSizeValidationAdvice(VariableSizeLimitProperties properties, ObjectMapper objectMapper) {
        this.validator = new VariableValueSizeValidator(
            properties.getMaxVariableValueLength(),
            properties.getWhitelistedVariables(),
            objectMapper
        );
    }

    @Override
    public boolean supports(
        MethodParameter methodParameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return targetType instanceof Class<?> clazz && findExtractor(clazz).isPresent();
    }

    @Override
    public Object afterBodyRead(
        Object body,
        HttpInputMessage inputMessage,
        MethodParameter parameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        findExtractor(body.getClass()).ifPresent(extractor -> validator.validate(extractor.apply(body)));
        return body;
    }

    private static Optional<Function<Object, Map<String, Object>>> findExtractor(Class<?> payloadType) {
        return VARIABLE_EXTRACTORS.entrySet()
            .stream()
            .filter(entry -> entry.getKey().isAssignableFrom(payloadType))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private static String extractVariableNameFromUri() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object uriVars = attrs.getRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriVars instanceof Map<?, ?> map) {
                Object value = map.get("variableName");
                return value instanceof String s ? s : null;
            }
        }
        return null;
    }
}
