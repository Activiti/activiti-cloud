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
package org.activiti.cloud.connectors.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.connectors.starter.test.it.RuntimeMockStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import reactor.core.publisher.Flux;

@Configuration
public class RuntimeMockStreamsConfiguration implements RuntimeMockStreams {

    @Autowired
    private ObjectMapper mapper;

    @Bean(RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    @ConditionalOnMissingBean(name = RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    @Override
    public SubscribableChannel integrationResultsConsumer() {
        return MessageChannels.publishSubscribe(RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
            .get();
    }


    @Bean(RuntimeMockStreams.INTEGRATION_EVENT_PRODUCER)
    @ConditionalOnMissingBean(name = RuntimeMockStreams.INTEGRATION_EVENT_PRODUCER)
    @Override
    public MessageChannel integrationEventsProducer() {
        return MessageChannels.direct(RuntimeMockStreams.INTEGRATION_EVENT_PRODUCER).get();
    }


    @FunctionBinding(output = RuntimeMockStreams.INTEGRATION_EVENT_PRODUCER)
    @ConditionalOnMissingBean(name = "integrationEventsSupplier")
    @Bean
    public Supplier<Flux<Message<?>>> integrationEventsSupplier() {
        return () -> Flux.from(IntegrationFlows.from(integrationEventsProducer())
            .log(LoggingHandler.Level.INFO,"integrationEventsSupplier")
            .toReactivePublisher());
    }

    @Bean(RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
    @ConditionalOnMissingBean(name = RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
    @Override
    public SubscribableChannel integrationErrorConsumer() {
        return MessageChannels.publishSubscribe(RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
            .get();
    }
}
