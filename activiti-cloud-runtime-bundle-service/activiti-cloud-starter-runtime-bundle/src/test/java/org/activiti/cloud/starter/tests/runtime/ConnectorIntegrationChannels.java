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
package org.activiti.cloud.starter.tests.runtime;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

public interface ConnectorIntegrationChannels {
    String INTEGRATION_EVENTS_CONSUMER = "integrationEventsConsumer";
    String REST_CONNECTOR_CONSUMER = "restConnectorConsumer";
    String VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER = "varMappingIntegrationEventsConsumer";
    String CONSTANTS_INTEGRATION_EVENTS_CONSUMER = "constantsIntegrationEventsConsumer";
    String MEALS_CONNECTOR_CONSUMER = "mealsConnectorConsumer";
    String VALUE_PROCESSOR_CONSUMER = "valueProcessorConsumer";

    @InputBinding(INTEGRATION_EVENTS_CONSUMER)
    default SubscribableChannel integrationEventsConsumer() {
        return MessageChannels.publishSubscribe(INTEGRATION_EVENTS_CONSUMER).getObject();
    }

    @InputBinding(VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER)
    default SubscribableChannel varMappingIntegrationEventsConsumer() {
        return MessageChannels.publishSubscribe(VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER).getObject();
    }

    @InputBinding(CONSTANTS_INTEGRATION_EVENTS_CONSUMER)
    default SubscribableChannel constantsIntegrationEventsConsumer() {
        return MessageChannels.publishSubscribe(CONSTANTS_INTEGRATION_EVENTS_CONSUMER).getObject();
    }

    @InputBinding(REST_CONNECTOR_CONSUMER)
    default SubscribableChannel restConnectorConsumer() {
        return MessageChannels.publishSubscribe(REST_CONNECTOR_CONSUMER).getObject();
    }

    @InputBinding(MEALS_CONNECTOR_CONSUMER)
    default SubscribableChannel mealsConnectorConsumer() {
        return MessageChannels.publishSubscribe(MEALS_CONNECTOR_CONSUMER).getObject();
    }

    @InputBinding(VALUE_PROCESSOR_CONSUMER)
    default SubscribableChannel valueProcessorConsumer() {
        return MessageChannels.publishSubscribe(VALUE_PROCESSOR_CONSUMER).getObject();
    }
}
