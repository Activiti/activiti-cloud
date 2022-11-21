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
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.connectors.starter.test.it.RuntimeMockStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import reactor.core.publisher.Flux;

@Configuration
public class RuntimeMockStreamsConfiguration implements RuntimeMockStreams {

    @Autowired
    private ObjectMapper mapper;

    @Bean
    @Override
    public SubscribableChannel integrationResultsConsumer() {
        return MessageChannels.publishSubscribe(RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
            .get();
    }

    @Scope("singleton")
    @Bean
    @Override
    public MessageChannel integrationEventsProducer() {
        return MessageChannels.direct(RuntimeMockStreams.INTEGRATION_EVENT_PRODUCER).get();
    }

    @Scope("singleton")
    @FunctionBinding(output = RuntimeMockStreams.INTEGRATION_EVENT_PRODUCER)
    @Bean
    public Supplier<Flux<Message<?>>> integrationEventsSupplier(MessageChannel integrationEventsProducer) {
        return () -> Flux.from(IntegrationFlows.from(integrationEventsProducer)
            .log(LoggingHandler.Level.INFO,"integrationEventsSupplier")
            .toReactivePublisher());
    }

    @Bean
    @Override
    public SubscribableChannel integrationErrorConsumer() {
        return MessageChannels.publishSubscribe(RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
            .get();
    }
}
