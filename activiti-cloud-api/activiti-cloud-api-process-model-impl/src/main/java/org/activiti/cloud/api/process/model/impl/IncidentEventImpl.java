/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.api.process.model.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IncidentEvent;

public class IncidentEventImpl extends CloudRuntimeEventImpl<ProcessInstance, ProcessEvents> implements IncidentEvent {

    private String errorCode;
    private String errorMessage;
    private List<StackTraceElement> stackTraceElements;
    private String errorClassName;

    IncidentEventImpl() {}

    public IncidentEventImpl(Throwable error) {
        this.errorClassName = error.getClass().getName();
        this.errorCode =
            Optional
                .of(error)
                .filter(CloudBpmnError.class::isInstance)
                .map(CloudBpmnError.class::cast)
                .map(CloudBpmnError::getErrorCode)
                .orElse(null);

        Throwable cause = findRootCause(error);

        this.errorMessage = cause.getMessage();
        this.stackTraceElements = Arrays.asList(cause.getStackTrace());
    }

    @Override
    public ProcessEvents getEventType() {
        return ProcessEvents.PROCESS_CANCELLED;
    }

    @Override
    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getErrorClassName() {
        return errorClassName;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(errorClassName, errorMessage, stackTraceElements);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IncidentEventImpl other = (IncidentEventImpl) obj;
        return (
            Objects.equals(errorClassName, other.errorClassName) &&
            Objects.equals(errorMessage, other.errorMessage) &&
            Objects.equals(stackTraceElements, other.stackTraceElements)
        );
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder
            .append("IncidentErrorImpl [errorMessage=")
            .append(errorMessage)
            .append(", stackTraceElements=")
            .append(
                stackTraceElements != null
                    ? stackTraceElements.subList(0, Math.min(stackTraceElements.size(), maxLen))
                    : null
            )
            .append(", errorClassName=")
            .append(errorClassName)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }

    protected Throwable findRootCause(Throwable throwable) {
        Throwable rootCause = Objects.requireNonNull(throwable);

        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}
