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
package org.activiti.cloud.starter.rb.configuration;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class ProcessEngineChannelsConfiguration implements ProcessEngineChannels {

    @Bean(ProcessEngineChannels.COMMAND_CONSUMER)
    @Override
    public SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(ProcessEngineChannels.COMMAND_CONSUMER)
            .get();
    }

    @Bean(ProcessEngineChannels.COMMAND_RESULTS)
    @Override
    public MessageChannel commandResults() {
        return MessageChannels.direct(ProcessEngineChannels.COMMAND_RESULTS)
            .get();
    }

    @FunctionBinding(output = ProcessEngineChannels.COMMAND_RESULTS)
    @Bean
    public Supplier<Flux<Message<?>>> commandResultsSupplier(@Qualifier(ProcessEngineChannels.COMMAND_RESULTS) MessageChannel commandResults) {
        return () -> Flux.from(IntegrationFlows.from(commandResults)
            .log(LoggingHandler.Level.INFO,"commandResults")
            .toReactivePublisher());
    }

    @Bean
    @Override
    public MessageChannel auditProducer() {
        return MessageChannels.direct(ProcessEngineChannels.AUDIT_PRODUCER)
            .get();
    }

    @FunctionBinding(output = ProcessEngineChannels.AUDIT_PRODUCER)
    @Bean
    public Supplier<Flux<Message<?>>> auditProducerSupplier(MessageChannel auditProducer) {
        return () -> Flux.from(IntegrationFlows.from(auditProducer)
            .log(LoggingHandler.Level.INFO,"auditSupplier")
            .toReactivePublisher());
    }
}
