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

import java.util.function.BiConsumer;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.integration.channel.ChannelDispatcherAccessor;
import org.springframework.integration.channel.DirectChannel;

public class OutboxChannelConfigurer implements BiConsumer<OutboxChannelInterceptor, DirectChannel> {

    @Override
    public void accept(OutboxChannelInterceptor channelInterceptor, DirectChannel messageChannel) {
        final var dispatcher = ChannelDispatcherAccessor.getDispatcher(messageChannel);
        final var outboxLoadBalancingStrategy = new OutboxLoadBalancingStrategy(channelInterceptor);

        dispatcher.setLoadBalancingStrategy(outboxLoadBalancingStrategy);
        dispatcher.setFailover(false);
        dispatcher.addHandler(channelInterceptor);

        if (messageChannel instanceof DirectWithAttributesChannel) {
            // workaround to bypass this.getDispatcher().getHandlerCount() == 1 condition in subscribe method
            // See https://github.com/spring-cloud/spring-cloud-stream/commit/6fc3b85481f9af8a217740a7518555e85ce2025a#diff-73a587c245940bd82a1326a04669dd892f1dc33ff95b9440bcda507d3d0701cf
            dispatcher.addHandler(outboxLoadBalancingStrategy);
        }

        messageChannel.addInterceptor(channelInterceptor);
    }
}
