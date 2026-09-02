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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.services.core.validation.VariableProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.support.MethodParameter;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
public class VariableRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final VariableProperties variableProperties;

    public VariableRequestBodyAdvice(VariableProperties variableProperties) {
        this.variableProperties = variableProperties;
    }

    @Override
    public boolean supports(
        MethodParameter methodParameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return (
            targetType == SetProcessVariablesPayload.class ||
            targetType == CreateTaskVariablePayload.class ||
            targetType == UpdateTaskVariablePayload.class
        );
    }

    @Override
    public HttpInputMessage beforeBodyRead(
        HttpInputMessage inputMessage,
        MethodParameter parameter,
        Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        InputStream body = new BoundedInputStream(inputMessage.getBody(), variableProperties.getMaxRequestSize());
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return body;
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    private static class BoundedInputStream extends FilterInputStream {

        private final int limit;
        private long count;

        private BoundedInputStream(InputStream inputStream, int limit) {
            super(inputStream);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                increment(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                increment(read);
            }
            return read;
        }

        @Override
        public long skip(long amount) throws IOException {
            long skipped = super.skip(amount);
            if (skipped > 0) {
                increment(skipped);
            }
            return skipped;
        }

        private void increment(long amount) throws IOException {
            count += amount;
            if (limit > 0 && count > limit) {
                throw new IOException("Request body exceeds the configured maximum size");
            }
        }
    }
}
