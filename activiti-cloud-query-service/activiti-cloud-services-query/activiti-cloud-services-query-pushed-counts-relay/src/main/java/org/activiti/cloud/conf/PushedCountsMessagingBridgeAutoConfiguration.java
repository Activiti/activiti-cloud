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

import java.util.function.Consumer;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.CountConsumer;
import org.activiti.cloud.services.query.app.CountConsumerChannels;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.Message;
import reactor.core.publisher.Sinks;

/**
 * Wires the broker-side half of the pushed-counts relay that {@code activiti-cloud-starter-query-rest}
 * deliberately leaves unwired: a {@code pushedCountsSink} bean with no input binding, because that
 * bare REST starter carries no messaging binder. Deliberately its own small module - not folded into
 * either query-rest or query-consumer, and with no compile-time dependency on either - so that any
 * app combining query-rest with a binder-carrying module can pull this in independently, the same way
 * {@code EngineEventsConsumerChannels} works for engine events. {@code activiti-cloud-starter-query}
 * depends on it directly; a split rest/consumer deployment (query-rest + a separate binder-carrying
 * module in the same process) needs to add the same dependency itself.
 *
 * <p>Ordered after {@code QueryRestPushedCountsWebSocketAutoConfiguration} by name (no compile-time
 * class reference - that class lives in query-rest, not on this module's classpath) purely so the
 * sink bean it creates exists before this class's {@code @Bean} method looks for it.
 *
 * <p>{@link CountConsumerChannels} (imported via {@link CountConsumerChannelsConfiguration}) supplies
 * the {@code countConsumer} channel bean that {@code @FunctionBinding} attaches to - without it,
 * Spring Cloud Stream never declares a real binding on the broker for this function, no matter how
 * the destination property is set.
 */
@AutoConfiguration(afterName = "org.activiti.cloud.conf.QueryRestPushedCountsWebSocketAutoConfiguration")
@ConditionalOnProperty(name = "activiti.cloud.query.pushed-counts.enabled", havingValue = "true")
@PropertySource("classpath:pushed-counts-messaging.properties")
@Import(CountConsumerChannelsConfiguration.class)
public class PushedCountsMessagingBridgeAutoConfiguration {

    @Bean
    @FunctionBinding(input = CountConsumerChannels.COUNT_CONSUMER)
    public Consumer<Message<CountChangedMessage>> countConsumerFunction(
        Sinks.Many<CountChangedMessage> pushedCountsSink,
        FeatureToggle featureToggle
    ) {
        return new CountConsumer(pushedCountsSink, featureToggle);
    }
}
