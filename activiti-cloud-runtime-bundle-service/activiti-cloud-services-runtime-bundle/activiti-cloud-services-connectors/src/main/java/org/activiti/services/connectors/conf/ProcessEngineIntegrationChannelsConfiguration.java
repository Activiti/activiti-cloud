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
package org.activiti.services.connectors.conf;

import org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

@Configuration
public class ProcessEngineIntegrationChannelsConfiguration implements ProcessEngineIntegrationChannels {

    @Bean(ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER)
    @ConditionalOnMissingBean(name = ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER)
    @Override
    public SubscribableChannel integrationResultsConsumer() {
        return MessageChannels.publishSubscribe(ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER)
            .get();
    }

    @Bean(ProcessEngineIntegrationChannels.INTEGRATION_ERRORS_CONSUMER)
    @ConditionalOnMissingBean(name = ProcessEngineIntegrationChannels.INTEGRATION_ERRORS_CONSUMER)
    @Override
    public SubscribableChannel integrationErrorsConsumer() {
        return MessageChannels.publishSubscribe(ProcessEngineIntegrationChannels.INTEGRATION_ERRORS_CONSUMER)
            .get();
    }
}
