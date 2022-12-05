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
package org.activiti.cloud.common.messaging.config.test;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import reactor.core.publisher.Flux;

@TestConfiguration
public class TestBindingsChannelsConfiguration implements TestBindingsChannels {

    @Bean(COMMAND_CONSUMER)
    @Override
    public SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(COMMAND_CONSUMER).get();
    }

    @Bean(QUERY_CONSUMER)
    @Override
    public SubscribableChannel queryConsumer() {
        return MessageChannels.publishSubscribe(QUERY_CONSUMER).get();
    }

    @Bean(AUDIT_CONSUMER)
    @Override
    public SubscribableChannel auditConsumer() {
        return MessageChannels.publishSubscribe(AUDIT_CONSUMER).get();
    }

    @Bean(COMMAND_RESULTS)
    @Override
    public MessageChannel commandResults() {
        return MessageChannels.direct(COMMAND_RESULTS).get();
    }

    @Bean(AUDIT_PRODUCER)
    @Override
    public MessageChannel auditProducer() {
        return MessageChannels.direct(AUDIT_PRODUCER).get();
    }

    @FunctionBinding(output = TestBindingsChannels.AUDIT_PRODUCER)
    @Bean
    public Supplier<Flux<Message<?>>> auditProducerSupplier() {
        return () -> Flux.from(IntegrationFlows.from(auditProducer())
            .log(LoggingHandler.Level.INFO,"auditProducerSupplier")
            .toReactivePublisher());
    }

    @FunctionBinding(output = TestBindingsChannels.COMMAND_RESULTS)
    @Bean
    public Supplier<Flux<Message<?>>> commandResultsSupplier(TestBindingsChannels channels) {
        return () -> Flux.from(IntegrationFlows.from(commandResults())
            .log(LoggingHandler.Level.INFO,"commandResultsSupplier")
            .toReactivePublisher());
    }

}
