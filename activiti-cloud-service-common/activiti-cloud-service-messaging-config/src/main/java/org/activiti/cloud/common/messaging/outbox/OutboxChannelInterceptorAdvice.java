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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

public class OutboxChannelInterceptorAdvice implements MethodInterceptor {

    private final ChannelInterceptor channelInterceptor;
    private final MessageChannel messageChannel;
    private final String beanName;

    private static final Logger log = LoggerFactory.getLogger(OutboxChannelInterceptorAdvice.class);

    public OutboxChannelInterceptorAdvice(
        ChannelInterceptor channelInterceptor,
        MessageChannel messageChannel,
        String beanName
    ) {
        this.channelInterceptor = channelInterceptor;
        this.messageChannel = messageChannel;
        this.beanName = beanName;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // Filter exclusively for the "send" method
        if ("send".equals(invocation.getMethod().getName())) {
            Message<?> message = (Message<?>) invocation.getArguments()[0];

            log.debug("Outbox proxy intercepted send message {} on channel: {}", message, beanName);

            final var result = channelInterceptor.preSend(message, messageChannel);

            if (result == null) {
                log.debug("Outbox proxy message {} sent successfully via outbox: {}", message, beanName);
                return true;
            } else {
                // Execute the real send() method
                log.debug("Outbox proxy message {} sent to: {}", message, beanName);
                return invocation.proceed();
            }
        }

        // Route all other interface methods natively
        return invocation.proceed();
    }
}
