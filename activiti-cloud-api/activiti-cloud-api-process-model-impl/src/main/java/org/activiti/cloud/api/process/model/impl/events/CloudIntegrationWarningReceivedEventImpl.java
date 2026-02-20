package org.activiti.cloud.api.process.model.impl.events;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationWarningReceivedEvent;

public class CloudIntegrationWarningReceivedEventImpl
    extends CloudIntegrationEventImpl
    implements CloudIntegrationWarningReceivedEvent {

    private static final long serialVersionUID = 1L;

    private String warningCode;
    private String warningMessage;
    private String warningClassName;
    private List<StackTraceElement> stackTraceElements;

    public CloudIntegrationWarningReceivedEventImpl() {}

    public CloudIntegrationWarningReceivedEventImpl(
        IntegrationContext integrationContext,
        String warningCode,
        String warningMessage
    ) {
        super(integrationContext);
        this.warningCode = warningCode;
        this.warningMessage = warningMessage;
        this.warningClassName = null;
        this.stackTraceElements = Collections.emptyList();
    }

    public CloudIntegrationWarningReceivedEventImpl(
        String id,
        Long timestamp,
        IntegrationContext integrationContext,
        String warningCode,
        String warningMessage,
        String warningClassName,
        List<StackTraceElement> stackTraceElements
    ) {
        super(id, timestamp, integrationContext);
        this.warningCode = warningCode;
        this.warningMessage = warningMessage;
        this.warningClassName = warningClassName;
        this.stackTraceElements = stackTraceElements;
    }

    @Override
    public IntegrationEvent.IntegrationEvents getEventType() {
        return IntegrationEvent.IntegrationEvents.INTEGRATION_WARNING_RECEIVED;
    }

    @Override
    public String getWarningCode() {
        return warningCode;
    }

    @Override
    public String getWarningMessage() {
        return warningMessage;
    }

    @Override
    public String getWarningClassName() {
        return warningClassName;
    }

    @Override
    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(warningCode, warningMessage, warningClassName, stackTraceElements);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        CloudIntegrationWarningReceivedEventImpl other = (CloudIntegrationWarningReceivedEventImpl) obj;
        return (
            Objects.equals(warningCode, other.warningCode) &&
            Objects.equals(warningMessage, other.warningMessage) &&
            Objects.equals(warningClassName, other.warningClassName) &&
            Objects.equals(stackTraceElements, other.stackTraceElements)
        );
    }

    @Override
    public String toString() {
        return (
            "CloudIntegrationWarningReceivedEventImpl [warningCode=" +
            warningCode +
            ", warningMessage=" +
            warningMessage +
            "]"
        );
    }
}
