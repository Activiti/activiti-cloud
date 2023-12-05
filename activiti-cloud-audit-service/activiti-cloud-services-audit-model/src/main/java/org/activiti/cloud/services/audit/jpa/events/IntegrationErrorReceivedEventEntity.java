/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.ListOfStackTraceElementsJpaJsonConverter;

@Entity(name = IntegrationErrorReceivedEventEntity.INTEGRATION_ERROR_RECEIVED_EVENT)
@DiscriminatorValue(value = IntegrationErrorReceivedEventEntity.INTEGRATION_ERROR_RECEIVED_EVENT)
public class IntegrationErrorReceivedEventEntity extends IntegrationEventEntity {

    private static final int ERROR_MESSAGE_LENGTH = 255;

    protected static final String INTEGRATION_ERROR_RECEIVED_EVENT = "IntegrationErrorReceivedEvent";

    private String errorCode;

    @Column(length = ERROR_MESSAGE_LENGTH)
    private String errorMessage;

    private String errorClassName;

    @Convert(converter = ListOfStackTraceElementsJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private List<StackTraceElement> stackTraceElements;

    protected IntegrationErrorReceivedEventEntity() {}

    public IntegrationErrorReceivedEventEntity(CloudIntegrationErrorReceivedEvent event) {
        super(event);
        this.errorCode = event.getErrorCode();
        this.errorMessage = StringUtils.truncate(event.getErrorMessage(), ERROR_MESSAGE_LENGTH);
        this.errorClassName = event.getErrorClassName();
        this.stackTraceElements = event.getStackTraceElements();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = StringUtils.truncate(errorMessage, ERROR_MESSAGE_LENGTH);
    }

    public String getErrorClassName() {
        return errorClassName;
    }

    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    public void setStackTraceElements(List<StackTraceElement> stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder
            .append("IntegrationErrorReceivedEventEntity [errorMessage=")
            .append(errorCode)
            .append(", errorCode=")
            .append(errorMessage)
            .append(", errorClassName=")
            .append(errorClassName)
            .append(", stackTraceElements=")
            .append(
                stackTraceElements != null
                    ? stackTraceElements.subList(0, Math.min(stackTraceElements.size(), maxLen))
                    : null
            )
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
