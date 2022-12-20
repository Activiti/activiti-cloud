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

import static org.activiti.cloud.common.messaging.utilities.InternalChannelHelper.INTERNAL_CHANNEL_PREFIX;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.connectors.starter.test.it.MockCloudRuntimeEventsChannels;
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
public class MockCloudRuntimeEventsChannelsConfiguration implements MockCloudRuntimeEventsChannels {

    private static final String INTERNAL_COMMAND_RESULTS = INTERNAL_CHANNEL_PREFIX + COMMAND_RESULTS;
    private static final String INTERNAL_AUDIT_PRODUCER = INTERNAL_CHANNEL_PREFIX + AUDIT_PRODUCER;

    @Bean(COMMAND_CONSUMER)
    @ConditionalOnMissingBean(name = COMMAND_CONSUMER)
    @Override
    public SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(COMMAND_CONSUMER)
            .get();
    }

    @Bean(INTERNAL_COMMAND_RESULTS)
    @ConditionalOnMissingBean(name = INTERNAL_COMMAND_RESULTS)
    @Override
    public MessageChannel commandResults() {
        return MessageChannels.direct(INTERNAL_COMMAND_RESULTS).get();
    }

    @Bean(INTERNAL_AUDIT_PRODUCER)
    @ConditionalOnMissingBean(name = INTERNAL_AUDIT_PRODUCER)
    @Override
    public MessageChannel auditProducer() {
        return MessageChannels.direct(INTERNAL_AUDIT_PRODUCER).get();
    }

    @FunctionBinding(output = MockCloudRuntimeEventsChannels.AUDIT_PRODUCER)
    @ConditionalOnMissingBean(name = "auditProducerSupplier")
    @Bean
    public Supplier<Flux<Message<?>>> auditProducerSupplier() {
        return () -> Flux.from(IntegrationFlows.from(auditProducer())
            .log(LoggingHandler.Level.INFO,"auditProducerSupplier")
            .toReactivePublisher());
    }
}
