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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.SubscribableChannel;

public class JobMessageInputChannelFactory {

    private final SubscribableChannelBindingTargetFactory bindingTargetFactory;
    private final BindingServiceProperties bindingServiceProperties;
    private final ConfigurableListableBeanFactory beanFactory;

    public JobMessageInputChannelFactory(
        SubscribableChannelBindingTargetFactory bindingTargetFactory,
        BindingServiceProperties bindingServiceProperties,
        ConfigurableListableBeanFactory beanFactory
    ) {
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingServiceProperties = bindingServiceProperties;
        this.beanFactory = beanFactory;
    }

    public SubscribableChannel createInputChannel(String consumerName, BindingProperties bindingProperties) {
        bindingServiceProperties.getBindings().put(consumerName, bindingProperties);
        SubscribableChannel channel = bindingTargetFactory.createInput(consumerName);
        beanFactory.registerSingleton(consumerName, channel);
        channel = (SubscribableChannel) beanFactory.initializeBean(channel, consumerName);

        return channel;
    }
}
