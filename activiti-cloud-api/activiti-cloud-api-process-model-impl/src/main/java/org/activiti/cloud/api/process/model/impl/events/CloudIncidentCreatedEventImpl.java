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
package org.activiti.cloud.api.process.model.impl.events;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IncidentContext;
import org.activiti.cloud.api.process.model.IncidentEvent;
import org.activiti.cloud.api.process.model.IncidentEvent.IncidentEventType;
import org.activiti.cloud.api.process.model.IncidentSeverity;

public class CloudIncidentCreatedEventImpl
    extends CloudRuntimeEventImpl<IncidentContext, IncidentEventType>
    implements IncidentEvent {

    private String errorCode;
    private String errorMessage;
    private List<StackTraceElement> stackTraceElements;
    private String errorClassName;
    private IncidentSeverity severity = IncidentSeverity.ERROR;

    public CloudIncidentCreatedEventImpl() {}

    public CloudIncidentCreatedEventImpl(Throwable error, IncidentContext incidentContext) {
        super(incidentContext);
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
        setEntityId(incidentContext.getExecutionId());
    }

    public CloudIncidentCreatedEventImpl(Throwable error, IncidentContext incidentContext, IncidentSeverity severity) {
        this(error, incidentContext);
        this.severity = Objects.requireNonNullElse(severity, IncidentSeverity.ERROR);
    }

    public CloudIncidentCreatedEventImpl(
        String id,
        Long timestamp,
        IncidentContext entity,
        String errorClassName,
        String errorCode,
        String errorMessage,
        List<StackTraceElement> stackTraceElements
    ) {
        super(id, timestamp, entity);
        this.errorClassName = errorClassName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.stackTraceElements = stackTraceElements;
    }

    public CloudIncidentCreatedEventImpl(
        String id,
        Long timestamp,
        IncidentContext entity,
        String errorClassName,
        String errorCode,
        String errorMessage,
        List<StackTraceElement> stackTraceElements,
        IncidentSeverity severity
    ) {
        this(id, timestamp, entity, errorClassName, errorCode, errorMessage, stackTraceElements);
        this.severity = Objects.requireNonNullElse(severity, IncidentSeverity.ERROR);
    }

    @Override
    public IncidentEventType getEventType() {
        return IncidentEventType.INCIDENT_CREATED;
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
    public IncidentSeverity getSeverity() {
        return severity;
    }

    @Override
    public void setSeverity(IncidentSeverity severity) {
        this.severity = Objects.requireNonNullElse(severity, IncidentSeverity.ERROR);
    }

    protected Throwable findRootCause(Throwable throwable) {
        Throwable rootCause = Objects.requireNonNull(throwable);

        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}
