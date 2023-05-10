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

import org.activiti.cloud.common.messaging.config.OutputBindingConfiguration;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.StreamProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.MessageChannel;

@AutoConfiguration
@ConditionalOnClass({OutputBindingConfiguration.class, BinderFactoryAutoConfiguration.class})
public class TestProducerAutoConfiguration implements StreamProducer {

    @Bean
    @ConditionalOnMissingBean
    public MyProducer myProducer(@Qualifier(PRODUCER) MessageChannel producer) {
        return new MyProducer(producer);
    }
}
