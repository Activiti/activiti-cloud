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
package org.activiti.cloud.services.messages.core.config;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Flux;

@Configuration
public class MessageConnectorProcessorConfiguration implements MessageConnectorProcessor {

    @Bean(MessageConnectorProcessor.INPUT)
    @Override
    public MessageChannel input() {
        return MessageChannels.direct(MessageConnectorProcessor.INPUT)
            .get();
    }

    @Bean(MessageConnectorProcessor.OUTPUT)
    @Override
    public MessageChannel output() {
        return MessageChannels.direct(MessageConnectorProcessor.OUTPUT)
            .get();
    }

    @Scope("singleton")
    @FunctionBinding(output = MessageConnectorProcessor.OUTPUT)
    @Bean
    public Supplier<Flux<Message<?>>> messageConnectorOutput(@Qualifier(MessageConnectorProcessor.OUTPUT) MessageChannel output) {
        return () -> Flux.from(IntegrationFlows.from(output)
            .log(LoggingHandler.Level.INFO,"messageConnectorOutput")
            .toReactivePublisher());
    }

}
