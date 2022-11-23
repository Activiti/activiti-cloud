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
package org.activiti.cloud.services.test;

import java.util.function.Function;
import java.util.logging.Level;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.StreamProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Configuration
public class TestProducerAutoConfiguration {

    @Configuration
    static class MyProducerConfiguration implements StreamProducer {

        @Bean(StreamProducer.PRODUCER)
        @Override
        @ConditionalOnMissingBean(name = StreamProducer.PRODUCER)
        public MessageChannel producer() {
            return MessageChannels.direct(StreamProducer.PRODUCER).get();
        }

        @Bean
        @ConditionalOnMissingBean
        public MyProducer myProducer(@Qualifier(StreamProducer.PRODUCER) MessageChannel producer) {
            return new MyProducer(producer);
        }

        @FunctionBinding(input = StreamProducer.PRODUCER, output = StreamProducer.PRODUCER)
        @ConditionalOnMissingBean(name = "myProducerSupplier")
        @Bean
        public Function<Flux<Message<?>>, Flux<Message<?>>> myProducerSupplier() {
            return flux -> flux
                .log("myProducer", Level.INFO);
        }

    }

}
