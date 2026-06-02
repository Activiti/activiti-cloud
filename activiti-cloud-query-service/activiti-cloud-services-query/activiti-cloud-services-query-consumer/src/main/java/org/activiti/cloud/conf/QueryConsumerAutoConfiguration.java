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

import java.util.List;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.app.QueryConsumerChannels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;

@AutoConfiguration
@Import(QueryConsumerChannelsConfiguration.class)
public class QueryConsumerAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryConsumerAutoConfiguration.class);
    private static final int VIRTUAL_NODES_PER_PARTITION = 256;
    public static final String PARTITIONED_QUERY_CONSUMER_INTEGRATION_FLOW_INPUT =
        "partitionedQueryConsumerIntegrationFlowInput";

    @Bean
    @FunctionBinding(input = QueryConsumerChannels.QUERY_CONSUMER)
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> queryConsumerFunction(
        IntegrationFlow partitionedQueryConsumerIntegrationFlow
    ) {
        return partitionedQueryConsumerIntegrationFlow.getInputChannel()::send;
    }

    @Bean
    @ConditionalOnMissingBean
    QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider() {
        return new RuntimeQueryConsumerPartitionedChannelCountProvider();
    }

    @Bean
    QueryConsumerPartitionedChannelKeySelector queryConsumerPartitionedChannelKeySelector(
        QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider
    ) {
        return new UUIDConsumerPartitionedChannelKeySelector(queryConsumerPartitionedChannelCountProvider.get());
    }

    @Bean
    public IntegrationFlow partitionedQueryConsumerIntegrationFlow(
        QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider,
        QueryConsumerPartitionedChannelKeySelector queryConsumerPartitionedChannelKeySelector,
        GenericHandler<List<CloudRuntimeEvent<?, ?>>> genericQueryConsumerChannelHandlerAdapter
    ) {
        return IntegrationFlow
            .from(PARTITIONED_QUERY_CONSUMER_INTEGRATION_FLOW_INPUT)
            .channel(
                MessageChannels
                    .partitioned(queryConsumerPartitionedChannelCountProvider.get())
                    .partitionKey(queryConsumerPartitionedChannelKeySelector)
            )
            .handle(genericQueryConsumerChannelHandlerAdapter)
            .get();
    }

    @Bean
    GenericHandler<List<CloudRuntimeEvent<?, ?>>> genericQueryConsumerChannelHandlerAdapter(
        QueryConsumerChannelHandler queryConsumerChannelHandler
    ) {
        return (events, headers) -> {
            LOGGER.debug(
                "Handling {} events with root process instance id {} on thread: {}",
                events.size(),
                headers.get("rootProcessInstanceId"),
                Thread.currentThread().getName()
            );

            queryConsumerChannelHandler.receive(events);

            return null;
        };
    }
}
