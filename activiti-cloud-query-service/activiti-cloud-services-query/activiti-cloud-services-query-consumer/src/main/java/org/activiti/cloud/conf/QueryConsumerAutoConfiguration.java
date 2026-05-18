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
package org.activiti.cloud.conf;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.app.QueryConsumerChannels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

@AutoConfiguration
@Import(QueryConsumerChannelsConfiguration.class)
public class QueryConsumerAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryConsumerAutoConfiguration.class);

    @FunctionBinding(input = QueryConsumerChannels.QUERY_CONSUMER)
    @Bean
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> queryConsumerFunction(
        QueryConsumerChannelHandler queryConsumerChannelHandler
    ) {
        LOGGER.info(
            "QUERY - binding queryConsumerFunction on input channel '{}'",
            QueryConsumerChannels.QUERY_CONSUMER
        );
        return message -> {
            List<CloudRuntimeEvent<?, ?>> payload = message.getPayload();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                    "QUERY - received message id={} with {} events, types={}, headers={}",
                    message.getHeaders().getId(),
                    payload != null ? payload.size() : 0,
                    payload != null ? payload.stream().map(e -> e.getEventType().name()).toList() : List.of(),
                    message.getHeaders()
                );
            }
            queryConsumerChannelHandler.receive(payload != null ? payload : Collections.emptyList());
        };
    }
}
