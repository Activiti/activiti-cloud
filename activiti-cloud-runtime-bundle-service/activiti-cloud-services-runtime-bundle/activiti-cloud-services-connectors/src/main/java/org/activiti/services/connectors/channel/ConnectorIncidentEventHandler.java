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

import java.util.function.Consumer;
import org.activiti.cloud.api.process.model.ConnectorIncidentEvent;
import org.activiti.cloud.services.events.services.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorIncidentEventHandler implements Consumer<ConnectorIncidentEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorIncidentEventHandler.class);

    private final IncidentService incidentService;

    public ConnectorIncidentEventHandler(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @Override
    public void accept(ConnectorIncidentEvent event) {
        LOGGER.debug(
            "Received connector incident event for process instance: {}",
            event.getIntegrationContext() != null ? event.getIntegrationContext().getProcessInstanceId() : "unknown"
        );
        incidentService.createAndSendIncidentEvent(
            event.getIntegrationContext(),
            event.getException(),
            event.getSeverity()
        );
    }
}
