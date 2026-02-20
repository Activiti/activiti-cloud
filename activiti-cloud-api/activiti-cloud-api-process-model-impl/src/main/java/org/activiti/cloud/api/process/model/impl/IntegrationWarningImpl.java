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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationWarning;

public class IntegrationWarningImpl extends CloudRuntimeEntityImpl implements IntegrationWarning {

    private IntegrationRequest integrationRequest;
    private IntegrationContext integrationContext;
    private String warningCode;
    private String warningMessage;
    private String warningClassName;
    private List<StackTraceElement> stackTraceElements;

    IntegrationWarningImpl() {}

    public IntegrationWarningImpl(IntegrationRequest integrationRequest, String warningCode, String warningMessage) {
        this.integrationRequest = integrationRequest;
        this.integrationContext = integrationRequest.getIntegrationContext();
        this.warningCode = warningCode;
        this.warningMessage = warningMessage;
        this.warningClassName = null;
        this.stackTraceElements = Collections.emptyList();
    }

    @Override
    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }

    public void setIntegrationContext(IntegrationContext integrationContext) {
        this.integrationContext = integrationContext;
    }

    @Override
    public IntegrationRequest getIntegrationRequest() {
        return integrationRequest;
    }

    public void setIntegrationRequest(IntegrationRequest integrationRequest) {
        this.integrationRequest = integrationRequest;
    }

    @Override
    public String getWarningCode() {
        return warningCode;
    }

    public void setWarningCode(String warningCode) {
        this.warningCode = warningCode;
    }

    @Override
    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    @Override
    public String getWarningClassName() {
        return warningClassName;
    }

    public void setWarningClassName(String warningClassName) {
        this.warningClassName = warningClassName;
    }

    @Override
    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    public void setStackTraceElements(List<StackTraceElement> stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
            prime *
            result +
            Objects.hash(
                warningCode,
                warningMessage,
                warningClassName,
                integrationContext,
                integrationRequest,
                stackTraceElements
            );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        IntegrationWarningImpl other = (IntegrationWarningImpl) obj;
        return (
            Objects.equals(warningCode, other.warningCode) &&
            Objects.equals(warningMessage, other.warningMessage) &&
            Objects.equals(warningClassName, other.warningClassName) &&
            Objects.equals(integrationContext, other.integrationContext) &&
            Objects.equals(integrationRequest, other.integrationRequest) &&
            Objects.equals(stackTraceElements, other.stackTraceElements)
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("IntegrationWarningImpl [warningCode=")
            .append(warningCode)
            .append(", warningMessage=")
            .append(warningMessage)
            .append("]");
        return builder.toString();
    }
}
