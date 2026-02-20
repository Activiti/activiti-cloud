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
package org.activiti.cloud.services.audit.jpa.events;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import org.activiti.cloud.api.process.model.events.CloudIntegrationWarningReceivedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.ListOfStackTraceElementsJpaJsonConverter;

@Entity(name = IntegrationWarningReceivedEventEntity.INTEGRATION_WARNING_RECEIVED_EVENT)
@DiscriminatorValue(value = IntegrationWarningReceivedEventEntity.INTEGRATION_WARNING_RECEIVED_EVENT)
public class IntegrationWarningReceivedEventEntity extends IntegrationEventEntity {

    private static final int WARNING_MESSAGE_LENGTH = 255;

    protected static final String INTEGRATION_WARNING_RECEIVED_EVENT = "IntegrationWarningReceivedEvent";

    private String errorCode;

    @Column(length = WARNING_MESSAGE_LENGTH)
    private String errorMessage;

    private String errorClassName;

    @Convert(converter = ListOfStackTraceElementsJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private List<StackTraceElement> stackTraceElements;

    protected IntegrationWarningReceivedEventEntity() {}

    public IntegrationWarningReceivedEventEntity(CloudIntegrationWarningReceivedEvent event) {
        super(event);
        this.errorCode = event.getWarningCode();
        this.errorMessage = StringUtils.truncate(event.getWarningMessage(), WARNING_MESSAGE_LENGTH);
        this.errorClassName = event.getWarningClassName();
        this.stackTraceElements = event.getStackTraceElements();
    }

    public String getWarningCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getWarningMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = StringUtils.truncate(errorMessage, WARNING_MESSAGE_LENGTH);
    }

    public String getWarningClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    public void setStackTraceElements(List<StackTraceElement> stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    @Override
    public String toString() {
        return (
            "IntegrationWarningReceivedEventEntity [warningCode=" + errorCode + ", warningMessage=" + errorMessage + "]"
        );
    }
}
