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

    private String warningCode;

    @Column(length = WARNING_MESSAGE_LENGTH)
    private String warningMessage;

    private String warningClassName;

    @Convert(converter = ListOfStackTraceElementsJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private List<StackTraceElement> stackTraceElements;

    protected IntegrationWarningReceivedEventEntity() {}

    public IntegrationWarningReceivedEventEntity(CloudIntegrationWarningReceivedEvent event) {
        super(event);
        this.warningCode = event.getWarningCode();
        this.warningMessage = event.getWarningMessage();
        this.warningClassName = event.getWarningClassName();
        this.stackTraceElements = event.getStackTraceElements();
    }

    // Getters and setters for warningCode, warningMessage, warningClassName, stackTraceElements

    public String getWarningCode() { return warningCode; }
    public void setWarningCode(String warningCode) { this.warningCode = warningCode; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public String getWarningClassName() { return warningClassName; }
    public void setWarningClassName(String warningClassName) { this.warningClassName = warningClassName; }
    public List<StackTraceElement> getStackTraceElements() { return stackTraceElements; }
    public void setStackTraceElements(List<StackTraceElement> stackTraceElements) { this.stackTraceElements = stackTraceElements; }

    @Override
    public String toString() {
        return "IntegrationWarningReceivedEventEntity [warningCode=" + warningCode
            + ", warningMessage=" + warningMessage + "]";
    }
}

