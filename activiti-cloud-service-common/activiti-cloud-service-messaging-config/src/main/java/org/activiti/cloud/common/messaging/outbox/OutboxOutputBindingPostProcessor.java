/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.outbox;

import java.util.Optional;
import java.util.function.BiFunction;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;

public class OutboxOutputBindingPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxOutputBindingPostProcessor.class);

    private final BiFunction<Message<?>, DirectChannel, String> beanNameDestinationResolver = (m, c) -> c.getBeanName();

    private final DefaultListableBeanFactory beanFactory;
    private final OutboxChannelInterceptorFactory outboxChannelInterceptorFactory;

    public OutboxOutputBindingPostProcessor(
        DefaultListableBeanFactory beanFactory,
        OutboxChannelInterceptorFactory outboxChannelInterceptorFactory
    ) {
        this.beanFactory = beanFactory;
        this.outboxChannelInterceptorFactory = outboxChannelInterceptorFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DirectChannel messageChannel) {
            return Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class))
                .map(outputBinding -> {
                    final ChannelInterceptor outboxOutputBindingChannelInterceptor =
                        outboxChannelInterceptorFactory.apply(beanName, beanNameDestinationResolver);

                    // Configure Spring's ProxyFactory
                    ProxyFactory proxyFactory = new ProxyFactory();
                    proxyFactory.setTarget(messageChannel);

                    // Forces Spring to use CGLIB subclassing instead of a JDK Dynamic Proxy
                    proxyFactory.setProxyTargetClass(true);

                    // Add the method interceptor to execute custom logic on .send()
                    proxyFactory.addAdvice(
                        new OutboxChannelInterceptorAdvice(
                            outboxOutputBindingChannelInterceptor,
                            messageChannel,
                            beanName
                        )
                    );

                    // Return the JDK Dynamic Proxy to the container
                    return proxyFactory;
                })
                .map(ProxyFactory::getProxy)
                .orElse(bean);
        }
        return bean;
    }
}
