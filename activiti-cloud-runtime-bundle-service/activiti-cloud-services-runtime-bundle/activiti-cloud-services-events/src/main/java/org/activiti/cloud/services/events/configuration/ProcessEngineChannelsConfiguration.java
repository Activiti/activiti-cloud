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

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import reactor.core.publisher.Flux;

@Configuration
@Primary
public class ProcessEngineChannelsConfiguration implements ProcessEngineChannels {

    private static final String COMMAND_RESULTS_INTERNAL = COMMAND_RESULTS + "Internal";
    private static final String AUDIT_PRODUCER_INTERNAL = AUDIT_PRODUCER + "Internal";

    @Bean(ProcessEngineChannels.COMMAND_CONSUMER)
    @ConditionalOnMissingBean(name = ProcessEngineChannels.COMMAND_CONSUMER)
    @Override
    public SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(ProcessEngineChannels.COMMAND_CONSUMER)
            .get();
    }

    @Bean(COMMAND_RESULTS_INTERNAL)
    @ConditionalOnMissingBean(name = COMMAND_RESULTS_INTERNAL)
    @Override
    public MessageChannel commandResults() {
        return MessageChannels.direct(COMMAND_RESULTS_INTERNAL).get();
    }

    @FunctionBinding(output = ProcessEngineChannels.COMMAND_RESULTS)
    @ConditionalOnMissingBean(name = "commandResultsSupplier")
    @Bean
    public Supplier<Flux<Message<Object>>> commandResultsSupplier() {
        return () -> Flux.from(IntegrationFlows.from(commandResults())
                .log(LoggingHandler.Level.INFO, "commandResultsSupplier")
                .toReactivePublisher());
    }

    @Bean(AUDIT_PRODUCER_INTERNAL)
    @ConditionalOnMissingBean(name = AUDIT_PRODUCER_INTERNAL)
    @Override
    public MessageChannel auditProducer() {
        return MessageChannels.direct(AUDIT_PRODUCER_INTERNAL)
            .get();
    }

    @FunctionBinding(output = ProcessEngineChannels.AUDIT_PRODUCER)
    @Bean("auditProducerSupplier")
    @ConditionalOnMissingBean(name = "auditProducerSupplier")
    public Supplier<Flux<Message<?>>> auditProducerSupplier(StreamBridge streamBridge) {
        // this supplier is different from the others because 'auditProducer' is transactional
        // using a Flux with a transactional producer would require a ReactiveTransactionManager

        return () -> Flux.from(IntegrationFlows.from(auditProducer())
            .log(LoggingHandler.Level.INFO, "auditProducerSupplier")
            .intercept(new ChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    streamBridge.send(ProcessEngineChannels.AUDIT_PRODUCER, message);
                    return null;
                }
            })
            .toReactivePublisher());
    }

}
