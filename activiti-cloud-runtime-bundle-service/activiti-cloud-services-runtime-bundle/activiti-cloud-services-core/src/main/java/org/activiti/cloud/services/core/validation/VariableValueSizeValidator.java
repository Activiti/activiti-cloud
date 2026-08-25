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
package org.activiti.cloud.services.core.validation;

import java.io.OutputStream;
import java.util.Objects;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class VariableValueSizeValidator {

    private final ObjectMapper objectMapper;
    private final VariableProperties variableProperties;

    public VariableValueSizeValidator(ObjectMapper objectMapper, VariableProperties variableProperties) {
        this.objectMapper = objectMapper;
        this.variableProperties = variableProperties;
    }

    public void validate(SetProcessVariablesPayload payload) {
        if (payload == null || payload.getVariables() == null || isDisabled()) {
            return;
        }

        payload.getVariables().forEach(this::validate);
    }

    public void validate(CreateTaskVariablePayload payload) {
        if (payload == null || isDisabled()) {
            return;
        }

        validate(payload.getName(), payload.getValue());
    }

    public void validate(UpdateTaskVariablePayload payload) {
        if (payload == null || isDisabled()) {
            return;
        }

        validate(payload.getName(), payload.getValue());
    }

    public void validate(String variableName, Object variableValue) {
        if (variableValue == null || isDisabled()) {
            return;
        }

        int maxValueSize = variableProperties.getMaxValueSize();
        long valueSize = getSerializedSize(variableValue, maxValueSize);
        if (valueSize > maxValueSize) {
            throw new VariableValueSizeLimitExceededException(variableName, maxValueSize);
        }
    }

    private boolean isDisabled() {
        return variableProperties.getMaxValueSize() <= 0;
    }

    private long getSerializedSize(Object variableValue, int maxValueSize) {
        try {
            CountingOutputStream outputStream = new CountingOutputStream(maxValueSize);
            objectMapper.writeValue(outputStream, variableValue);
            return outputStream.size();
        } catch (SizeLimitExceededSignal signal) {
            return signal.size;
        } catch (JacksonException e) {
            throw new VariableValueSerializationException(e);
        }
    }

    /**
     * Internal control-flow exception used to abort serialization as soon as the configured
     * limit is exceeded, avoiding the cost of serializing the remainder of the value.
     * Stack trace and suppression are disabled since this is not an error condition.
     */
    private static final class SizeLimitExceededSignal extends RuntimeException {

        private final long size;

        private SizeLimitExceededSignal(long size) {
            super(null, null, false, false);
            this.size = size;
        }
    }

    private static class CountingOutputStream extends OutputStream {

        private final int limit;
        private long size;

        private CountingOutputStream(int limit) {
            this.limit = limit;
        }

        @Override
        public void write(int b) {
            size++;
            checkLimit();
        }

        @Override
        public void write(byte[] b, int off, int len) {
            Objects.checkFromIndexSize(off, len, b.length);
            size += len;
            checkLimit();
        }

        private void checkLimit() {
            if (size > limit) {
                throw new SizeLimitExceededSignal(size);
            }
        }

        private long size() {
            return size;
        }
    }
}
