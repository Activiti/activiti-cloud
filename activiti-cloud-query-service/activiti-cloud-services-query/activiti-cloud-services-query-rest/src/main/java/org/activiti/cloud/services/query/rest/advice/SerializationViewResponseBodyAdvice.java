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
package org.activiti.cloud.services.query.rest.advice;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collections;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class SerializationViewResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    public static final String SERIALIZATION_VIEW_ATTRIBUTE =
        SerializationViewResponseBodyAdvice.class.getName() + ".serializationView";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return (
            AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType) &&
            currentSerializationView() != null
        );
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        return body;
    }

    @Override
    public Map<String, Object> determineWriteHints(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        Class<?> view = currentSerializationView();
        if (view != null) {
            return Collections.singletonMap(JsonView.class.getName(), view);
        }
        return Collections.emptyMap();
    }

    private Class<?> currentSerializationView() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object view = attributes.getAttribute(SERIALIZATION_VIEW_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (view instanceof Class<?> viewClass) {
                return viewClass;
            }
        }
        return null;
    }
}
