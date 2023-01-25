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

import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.StreamProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Flux;

@Configuration
public class TestProducerAutoConfiguration {

    @Configuration
    @ConditionalOnClass({ Flux.class, MessageChannels.class })
    static class MyProducerConfiguration implements StreamProducer {

        @OutputBinding(PRODUCER)
        @Override
        @ConditionalOnMissingBean(name = PRODUCER)
        public MessageChannel producer() {
            return MessageChannels.direct(PRODUCER).get();
        }

        @Bean
        @ConditionalOnMissingBean
        public MyProducer myProducer(@Qualifier(PRODUCER) MessageChannel producer) {
            return new MyProducer(producer);
        }

    }

}
