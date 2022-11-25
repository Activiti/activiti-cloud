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
package org.activiti.cloud.services.job.executor;

import org.activiti.cloud.common.messaging.config.ActivitiMessagingDestinationsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.activiti.asyncExecutorActivate", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(ActivitiMessagingDestinationsAutoConfiguration.class)
public class MessageBasedJobManagerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JobMessageBuilderFactory jobMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new JobMessageBuilderFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobMessageInputChannelFactory jobMessageInputChannelFactory(SubscribableChannelBindingTargetFactory bindingTargetFactory,
                                                                       BindingServiceProperties bindingServiceProperties,
                                                                       ConfigurableListableBeanFactory beanFactory) {
        return new JobMessageInputChannelFactory(bindingTargetFactory, bindingServiceProperties, beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageBasedJobManagerFactory messageBasedJobManagerFactory(BindingServiceProperties bindingServiceProperties,
                                                                       JobMessageProducer jobMessageProducer) {
        return new DefaultMessageBasedJobManagerFactory(bindingServiceProperties, jobMessageProducer);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobMessageProducer jobMessageProducer(StreamBridge streamBridge,
                                                 ApplicationEventPublisher eventPublisher,
                                                 JobMessageBuilderFactory jobMessageBuilderFactory) {
        return new DefaultJobMessageProducer(streamBridge,
                                             eventPublisher,
                                             jobMessageBuilderFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobMessageHandlerFactory jobMessageHandlerFactory() {
        return new DefaultJobMessageHandlerFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageBasedJobManagerConfigurator messageBasedJobManagerConfigurator(ConfigurableListableBeanFactory beanFactory,
                                                                                 BindingService bindingService,
                                                                                 JobMessageInputChannelFactory jobMessageInputChannelFactory,
                                                                                 MessageBasedJobManagerFactory messageBasedJobManagerFactory,
                                                                                 JobMessageHandlerFactory jobMessageHandlerFactory) {

        return new MessageBasedJobManagerConfigurator(beanFactory,
                                                      bindingService,
                                                      jobMessageInputChannelFactory,
                                                      messageBasedJobManagerFactory,
                                                      jobMessageHandlerFactory);
    }

}
