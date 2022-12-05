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
package org.activiti.services.subscription.config;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.services.subscription.channel.ProcessEngineSignalChannels;
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
public class ProcessEngineSignalChannelsConfiguration implements ProcessEngineSignalChannels {

    @Bean(ProcessEngineSignalChannels.SIGNAL_CONSUMER)
    @Override
    public SubscribableChannel signalConsumer() {
        return MessageChannels.publishSubscribe(ProcessEngineSignalChannels.SIGNAL_CONSUMER)
            .get();
    }

    @Bean(ProcessEngineSignalChannels.SIGNAL_PRODUCER)
    @Override
    public MessageChannel signalProducer() {
        return MessageChannels.direct(ProcessEngineSignalChannels.SIGNAL_PRODUCER)
            .get();
    }

    @FunctionBinding(output = ProcessEngineSignalChannels.SIGNAL_PRODUCER)
    @ConditionalOnMissingBean(name = "signalProducerSupplier")
    @Bean
    public Supplier<Flux<Message<?>>> signalProducerSupplier() {
        return () -> Flux.from(IntegrationFlows.from(signalProducer())
            .log(LoggingHandler.Level.INFO,"signalProducerSupplier")
            .toReactivePublisher());
    }

}
