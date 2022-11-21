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

import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
public class MessageClientStreamConfiguration implements MessageClientStream {

    @Bean
    @Override
    public MessageChannel myCmdProducer() {
        return MessageChannels.direct(MessageClientStream.MY_CMD_PRODUCER)
            .get();
    }

    @FunctionBinding(output = "")
    @Bean
    @ConditionalOnMissingBean(name = MessageClientStream.MY_CMD_PRODUCER + "Supplier")
    public Supplier<Flux<Message<?>>> messageConnectorOutput(MessageChannel myCmdProducer) {
        return () -> Flux.from(IntegrationFlows.from(myCmdProducer)
            .log(LoggingHandler.Level.INFO,"myCmdProducer")
            .toReactivePublisher());
    }

    @Bean
    @Override
    public SubscribableChannel myCmdResults() {
        return MessageChannels.publishSubscribe(MessageClientStream.MY_CMD_RESULTS)
            .get();
    }
}
