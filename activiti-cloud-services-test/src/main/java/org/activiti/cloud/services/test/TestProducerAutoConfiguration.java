/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.test;

import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.StreamProducer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

@Configuration
@AutoConfigureAfter(value = BindingServiceConfiguration.class)
public class TestProducerAutoConfiguration {

    @ConditionalOnBean(BindingService.class)
    @EnableBinding(StreamProducer.class)
    static class MyProducerConfiguration {
    
        @Bean
        @ConditionalOnMissingBean
        public MyProducer myProducer(MessageChannel producer) {
            return new MyProducer(producer);
        }
    }
    
}
