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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

@TestConfiguration()
public class ConnectorIntegrationChannelsConfiguration implements ConnectorIntegrationChannels{

    @Bean(INTEGRATION_EVENTS_CONSUMER)
    @ConditionalOnMissingBean(name = INTEGRATION_EVENTS_CONSUMER)
    @Override
    public SubscribableChannel integrationEventsConsumer() {
        return MessageChannels.publishSubscribe(INTEGRATION_EVENTS_CONSUMER)
            .get();
    }

    @Bean(VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER)
    @ConditionalOnMissingBean(name = VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER)
    @Override
    public SubscribableChannel varMappingIntegrationEventsConsumer() {
        return MessageChannels.publishSubscribe(VAR_MAPPING_INTEGRATION_EVENTS_CONSUMER)
            .get();
    }

    @Bean(CONSTANTS_INTEGRATION_EVENTS_CONSUMER)
    @ConditionalOnMissingBean(name = CONSTANTS_INTEGRATION_EVENTS_CONSUMER)
    @Override
    public SubscribableChannel constantsIntegrationEventsConsumer() {
        return MessageChannels.publishSubscribe(CONSTANTS_INTEGRATION_EVENTS_CONSUMER)
            .get();
    }

    @Bean(REST_CONNECTOR_CONSUMER)
    @ConditionalOnMissingBean(name = REST_CONNECTOR_CONSUMER)
    @Override
    public SubscribableChannel restConnectorConsumer() {
        return MessageChannels.publishSubscribe(REST_CONNECTOR_CONSUMER)
            .get();
    }

    @Bean(MEALS_CONNECTOR_CONSUMER)
    @ConditionalOnMissingBean(name = MEALS_CONNECTOR_CONSUMER)
    @Override
    public SubscribableChannel mealsConnectorConsumer() {
        return MessageChannels.publishSubscribe(MEALS_CONNECTOR_CONSUMER)
            .get();
    }

    @Bean(VALUE_PROCESSOR_CONSUMER)
    @ConditionalOnMissingBean(name = VALUE_PROCESSOR_CONSUMER)
    @Override
    public SubscribableChannel valueProcessorConsumer() {
        return MessageChannels.publishSubscribe(VALUE_PROCESSOR_CONSUMER)
            .get();
    }

}
