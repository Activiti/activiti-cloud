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

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionDefinition;
import org.activiti.cloud.connectors.starter.channels.ProcessRuntimeChannels;
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
public class ProcessRuntimeChannelsConfiguration implements ProcessRuntimeChannels {

    @Bean
    @Override
    public MessageChannel runtimeCmdProducer() {
        return MessageChannels.direct(ProcessRuntimeChannels.RUNTIME_CMD_PRODUCER)
            .get();
    }

    @FunctionDefinition(output = ProcessRuntimeChannels.RUNTIME_CMD_PRODUCER)
    @Bean
    public Supplier<Flux<Message<?>>> runtimeCmdSupplier() {
        return () -> Flux.from(IntegrationFlows.from(runtimeCmdProducer())
            .log(LoggingHandler.Level.INFO,"runtimeCmdSupplier")
            .toReactivePublisher());
    }

    @Bean
    @Override
    public SubscribableChannel runtimeCmdResults() {
        return MessageChannels.publishSubscribe(ProcessRuntimeChannels.RUNTIME_CMD_RESULTS)
            .get();
    }
}