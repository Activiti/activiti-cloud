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

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

public interface ProcessEngineIntegrationChannels {
    String INTEGRATION_RESULTS_CONSUMER = "integrationResultsConsumer";

    String INTEGRATION_ERRORS_CONSUMER = "integrationErrorsConsumer";

    String CONNECTOR_INCIDENT_CONSUMER = "connectorIncidentConsumer";

    @InputBinding(INTEGRATION_RESULTS_CONSUMER)
    default SubscribableChannel integrationResultsConsumer() {
        return MessageChannels.publishSubscribe(INTEGRATION_RESULTS_CONSUMER).getObject();
    }

    @InputBinding(INTEGRATION_ERRORS_CONSUMER)
    default SubscribableChannel integrationErrorsConsumer() {
        return MessageChannels.publishSubscribe(INTEGRATION_ERRORS_CONSUMER).getObject();
    }

    @InputBinding(CONNECTOR_INCIDENT_CONSUMER)
    default SubscribableChannel connectorIncidentConsumer() {
        return MessageChannels.publishSubscribe(CONNECTOR_INCIDENT_CONSUMER).getObject();
    }
}
