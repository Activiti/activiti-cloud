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
package org.activiti.services.connectors.channel;

import static org.mockito.Mockito.verify;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.ConnectorIncidentEvent;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.services.events.services.IncidentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorIncidentEventHandlerTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private IntegrationContext integrationContext;

    @InjectMocks
    private ConnectorIncidentEventHandler handler;

    @Test
    void should_delegateToIncidentService_when_eventReceived() {
        Exception exception = new RuntimeException("Invalid email addresses");
        ConnectorIncidentEvent event = new ConnectorIncidentEvent(
            integrationContext,
            exception,
            IncidentSeverity.WARNING
        );

        handler.accept(event);

        verify(incidentService).createAndSendIncidentEvent(integrationContext, exception, IncidentSeverity.WARNING);
    }

    @Test
    void should_delegateWithErrorSeverity_when_eventHasErrorSeverity() {
        Exception exception = new RuntimeException("Critical connector failure");
        ConnectorIncidentEvent event = new ConnectorIncidentEvent(
            integrationContext,
            exception,
            IncidentSeverity.ERROR
        );

        handler.accept(event);

        verify(incidentService).createAndSendIncidentEvent(integrationContext, exception, IncidentSeverity.ERROR);
    }

    @Test
    void should_delegateWithNullSeverity_when_eventHasNoSeverity() {
        Exception exception = new RuntimeException("Some error");
        ConnectorIncidentEvent event = new ConnectorIncidentEvent(integrationContext, exception, null);

        handler.accept(event);

        verify(incidentService).createAndSendIncidentEvent(integrationContext, exception, null);
    }

    @Test
    void should_logProcessInstanceId_when_integrationContextPresent() {
        Exception exception = new RuntimeException("test");
        ConnectorIncidentEvent event = new ConnectorIncidentEvent(
            integrationContext,
            exception,
            IncidentSeverity.WARNING
        );

        handler.accept(event);

        verify(incidentService).createAndSendIncidentEvent(integrationContext, exception, IncidentSeverity.WARNING);
    }

    @Test
    void should_handleNullIntegrationContext() {
        Exception exception = new RuntimeException("test");
        ConnectorIncidentEvent event = new ConnectorIncidentEvent(null, exception, IncidentSeverity.WARNING);

        handler.accept(event);

        verify(incidentService).createAndSendIncidentEvent(
            (IntegrationContext) null,
            exception,
            IncidentSeverity.WARNING
        );
    }
}
