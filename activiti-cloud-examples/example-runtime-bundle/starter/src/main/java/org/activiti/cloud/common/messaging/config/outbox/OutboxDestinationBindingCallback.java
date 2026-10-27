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
package org.activiti.cloud.common.messaging.config.outbox;

import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binding.NewDestinationBindingCallback;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

public class OutboxDestinationBindingCallback implements NewDestinationBindingCallback<Object> {

    private final ChannelInterceptor outboxChannelInterceptor;

    public OutboxDestinationBindingCallback(ChannelInterceptor outboxChannelInterceptor) {
        this.outboxChannelInterceptor = outboxChannelInterceptor;
    }

    @Override
    public void configure(
        String channelName,
        MessageChannel channel,
        ProducerProperties producerProperties,
        Object extendedProducerProperties
    ) {
        if (channel instanceof InterceptableChannel interceptableChannel) {
            interceptableChannel.addInterceptor(outboxChannelInterceptor);
        }
    }
}
