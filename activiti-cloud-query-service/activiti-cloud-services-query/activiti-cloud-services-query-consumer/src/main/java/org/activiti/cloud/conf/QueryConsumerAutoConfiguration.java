/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

@AutoConfiguration
@Import(QueryConsumerChannelsConfiguration.class)
public class QueryConsumerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(QueryConsumerAutoConfiguration.class);

    public QueryConsumerAutoConfiguration() {
        logger.warn("[QUERY-TRACE] ===== QueryConsumerAutoConfiguration constructor called =====");
    }

    @FunctionBinding(input = QueryConsumerChannels.QUERY_CONSUMER)
    @Bean
    public Consumer<List<CloudRuntimeEvent<?, ?>>> queryConsumerFunction(
        QueryConsumerChannelHandler queryConsumerChannelHandler
    ) {
        logger.warn("[QUERY-TRACE] ===== Consumer function bean CREATED - queryConsumerFunction =====");
        logger.warn("[QUERY-TRACE] Input binding: {}", QueryConsumerChannels.QUERY_CONSUMER);
        return events -> {
            logger.warn(
                "[QUERY-TRACE] ===== Consumer function INVOKED with {} events =====",
                events != null ? events.size() : 0
            );
            if (events != null && !events.isEmpty()) {
                logger.warn(
                    "[QUERY-TRACE] First event: eventType={}, processInstanceId={}",
                    events.get(0).getEventType(),
                    events.get(0).getProcessInstanceId()
                );
            }
            queryConsumerChannelHandler.receive(events);
        };
    }
}
