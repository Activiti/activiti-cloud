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
package org.activiti.cloud.api.process.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.activiti.api.process.model.IntegrationContext;

public class ConnectorIncidentEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private IntegrationContext integrationContext;
    private Exception exception;
    private IncidentSeverity severity;

    public ConnectorIncidentEvent() {}

    public ConnectorIncidentEvent(
        IntegrationContext integrationContext,
        Exception exception,
        IncidentSeverity severity
    ) {
        this.integrationContext = integrationContext;
        this.exception = exception;
        this.severity = severity;
    }

    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }

    public void setIntegrationContext(IntegrationContext integrationContext) {
        this.integrationContext = integrationContext;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectorIncidentEvent that = (ConnectorIncidentEvent) o;
        return (
            Objects.equals(integrationContext, that.integrationContext) &&
            Objects.equals(exception, that.exception) &&
            severity == that.severity
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(integrationContext, exception, severity);
    }

    @Override
    public String toString() {
        return "ConnectorIncidentEvent{" + "exception=" + exception + ", severity=" + severity + '}';
    }
}
