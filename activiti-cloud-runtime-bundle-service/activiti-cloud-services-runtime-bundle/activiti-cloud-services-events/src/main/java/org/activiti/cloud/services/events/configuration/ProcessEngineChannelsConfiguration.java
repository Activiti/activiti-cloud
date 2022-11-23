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
package org.activiti.cloud.services.events.configuration;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import reactor.core.publisher.Flux;

@Configuration
@Primary
public class ProcessEngineChannelsConfiguration implements ProcessEngineChannels {

    @Bean(ProcessEngineChannels.COMMAND_CONSUMER)
    @ConditionalOnMissingBean(name = ProcessEngineChannels.COMMAND_CONSUMER)
    @Override
    public SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(ProcessEngineChannels.COMMAND_CONSUMER)
            .get();
    }

    @Bean(ProcessEngineChannels.COMMAND_RESULTS)
    @ConditionalOnMissingBean(name = ProcessEngineChannels.COMMAND_RESULTS)
    @Override
    public MessageChannel commandResults() {
        return MessageChannels.direct(ProcessEngineChannels.COMMAND_RESULTS)
            .get();
    }

    @FunctionBinding(input = ProcessEngineChannels.COMMAND_RESULTS, output = ProcessEngineChannels.COMMAND_RESULTS)
    @ConditionalOnMissingBean(name = "commandResultsSupplier")
    @Bean
    public Function<Flux<Message<?>>, Flux<Message<?>>> commandResultsSupplier() {
        return flux -> flux
            .log("commandResults", Level.INFO);
    }

    @Bean(ProcessEngineChannels.AUDIT_PRODUCER)
    @ConditionalOnMissingBean(name = ProcessEngineChannels.AUDIT_PRODUCER)
    @Override
    public MessageChannel auditProducer() {
        return MessageChannels.direct(ProcessEngineChannels.AUDIT_PRODUCER)
            .get();
    }

    @FunctionBinding(input = ProcessEngineChannels.AUDIT_PRODUCER, output = ProcessEngineChannels.AUDIT_PRODUCER)
    @ConditionalOnMissingBean(name = "auditProducerSupplier")
    @Bean
    public Function<Flux<Message<?>>, Flux<Message<?>>> auditProducerSupplier() {
        return flux -> flux
            .log("auditSupplier", Level.INFO);
    }
}
