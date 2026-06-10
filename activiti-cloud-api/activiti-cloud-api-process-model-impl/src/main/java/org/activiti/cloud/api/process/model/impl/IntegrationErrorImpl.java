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
package org.activiti.cloud.api.process.model.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.SourceVersion;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.util.StringUtils;

public class IntegrationErrorImpl extends CloudRuntimeEntityImpl implements IntegrationError {

    private IntegrationRequest integrationRequest;
    private IntegrationContext integrationContext;

    private String errorCode;
    private String errorMessage;
    private List<StackTraceElement> stackTraceElements;
    private String errorClassName;

    @JsonCreator
    public IntegrationErrorImpl() {}

    public IntegrationErrorImpl(IntegrationRequest integrationRequest, Throwable error) {
        this.integrationRequest = integrationRequest;
        this.integrationContext = integrationRequest.getIntegrationContext();
        this.errorClassName = error.getClass().getName();
        this.errorCode =
            Optional
                .of(error)
                .filter(CloudBpmnError.class::isInstance)
                .map(CloudBpmnError.class::cast)
                .map(CloudBpmnError::getErrorCode)
                .orElse(null);

        Throwable cause = findRootCause(error);

        this.errorMessage = this.getDetailedErrorMessage(error);
        this.stackTraceElements = Arrays.asList(cause.getStackTrace());
    }

    public IntegrationErrorImpl(IntegrationRequest integrationRequest, Throwable error, String customErrorMessage) {
        this.integrationRequest = integrationRequest;
        this.integrationContext = integrationRequest.getIntegrationContext();
        this.errorClassName = error.getClass().getName();
        this.errorCode =
            Optional
                .of(error)
                .filter(CloudBpmnError.class::isInstance)
                .map(CloudBpmnError.class::cast)
                .map(CloudBpmnError::getErrorCode)
                .orElse(null);

        Throwable cause = findRootCause(error);

        this.errorMessage =
            StringUtils.hasText(customErrorMessage) ? customErrorMessage : this.getDetailedErrorMessage(error);
        this.stackTraceElements = Arrays.asList(cause.getStackTrace());
    }

    @Override
    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }

    @Override
    public IntegrationRequest getIntegrationRequest() {
        return integrationRequest;
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

    public void setIntegrationRequest(IntegrationRequest integrationRequest) {
        this.integrationRequest = integrationRequest;
    }

    public void setIntegrationContext(IntegrationContext integrationContext) {
        this.integrationContext = integrationContext;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setStackTraceElements(List<StackTraceElement> stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
            prime *
            result +
            Objects.hash(errorClassName, errorMessage, integrationContext, integrationRequest, stackTraceElements);
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
        IntegrationErrorImpl other = (IntegrationErrorImpl) obj;
        return (
            Objects.equals(errorClassName, other.errorClassName) &&
            Objects.equals(errorMessage, other.errorMessage) &&
            Objects.equals(integrationContext, other.integrationContext) &&
            Objects.equals(integrationRequest, other.integrationRequest) &&
            Objects.equals(stackTraceElements, other.stackTraceElements)
        );
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder
            .append("IntegrationErrorImpl [integrationRequest=")
            .append(integrationRequest)
            .append(", integrationContext=")
            .append(integrationContext)
            .append(", errorMessage=")
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

    private String getDetailedErrorMessage(Throwable error) {
        var message = error.getMessage();
        var rootCause = this.findRootCause(error);
        var rootMessage = rootCause.getMessage();

        if (StringUtils.hasText(rootMessage)) {
            if (this.isJsonFormat(rootMessage)) {
                return rootMessage;
            }
            rootMessage = this.removeClassNameFromErrorMessage(rootMessage);
        }

        if (StringUtils.hasText(message)) {
            if (this.isJsonFormat(message)) {
                return message;
            }
            message = this.removeClassNameFromErrorMessage(message);
        }

        if (!StringUtils.hasText(message)) {
            return rootMessage;
        }
        if (!StringUtils.hasText(rootMessage)) {
            return message;
        }

        if (rootMessage.toLowerCase().contains(message.toLowerCase())) {
            return rootMessage;
        }
        if (message.toLowerCase().contains(rootMessage.toLowerCase())) {
            return message;
        }

        return message + " caused by: " + rootMessage;
    }

    private boolean isJsonFormat(String message) {
        return message.startsWith("{") && message.endsWith("}");
    }

    private String removeClassNameFromErrorMessage(String message) {
        int endIndex = message.indexOf(":");
        if (this.startsWithClassName(message, endIndex)) {
            var messageWithoutClassName = message.substring(endIndex + 1);
            if (StringUtils.hasText(messageWithoutClassName)) {
                return messageWithoutClassName.trim();
            }
            return null;
        }
        return message;
    }

    private boolean startsWithClassName(String message, int endIndex) {
        return endIndex != -1 && isStrictFQCN(message.substring(0, endIndex));
    }

    private boolean isStrictFQCN(String fqcn) {
        if (!StringUtils.hasText(fqcn)) return false;

        fqcn = fqcn.trim();
        if (fqcn.startsWith(".") || fqcn.endsWith(".")) return false;

        String[] parts = fqcn.split("\\.");
        if (parts.length < 2) return false;

        return isValidFQCN(parts);
    }

    private boolean isValidFQCN(String... parts) {
        for (int i = 0; i < parts.length - 1; i++) {
            var part = parts[i];
            if (
                !StringUtils.hasText(part) || !part.equals(part.toLowerCase()) || !isValidIdentifier(part)
            ) return false;
        }

        String className = parts[parts.length - 1];
        return Character.isUpperCase(className.charAt(0)) && isValidIdentifier(className);
    }

    private boolean isValidIdentifier(String name) {
        return StringUtils.hasText(name) && SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name);
    }
}
