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
package org.activiti.cloud.starter.tests.cmdendpoint;

import static org.activiti.cloud.common.messaging.utilities.InternalChannelHelper.INTERNAL_CHANNEL_PREFIX;

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
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
public class MessageClientStreamConfiguration implements MessageClientStream {

    private static final String INTERNAL_MY_CMD_PRODUCER = INTERNAL_CHANNEL_PREFIX + MY_CMD_PRODUCER;

    @Bean(INTERNAL_MY_CMD_PRODUCER)
    @ConditionalOnMissingBean(name = INTERNAL_MY_CMD_PRODUCER)
    @Override
    public MessageChannel myCmdProducer() {
        return MessageChannels.direct(INTERNAL_MY_CMD_PRODUCER)
            .get();
    }

    @FunctionBinding(output = MY_CMD_PRODUCER)
    @ConditionalOnMissingBean(name = "messageConnectorOutput")
    @Bean
    public Supplier<Flux<Message<Object>>> messageConnectorOutput() {
        return () -> Flux.from(IntegrationFlows.from(myCmdProducer())
            .log(LoggingHandler.Level.INFO,"myCmdProducer")
            .toReactivePublisher());
    }

    @Bean(MY_CMD_RESULTS)
    @ConditionalOnMissingBean(name = MY_CMD_RESULTS)
    @Override
    public SubscribableChannel myCmdResults() {
        return MessageChannels.publishSubscribe(MY_CMD_RESULTS)
            .get();
    }
}
