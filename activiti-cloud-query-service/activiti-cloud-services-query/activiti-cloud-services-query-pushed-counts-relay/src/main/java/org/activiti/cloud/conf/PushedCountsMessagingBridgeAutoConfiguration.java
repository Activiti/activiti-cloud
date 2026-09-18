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
 * Standalone module (no dependency on query-rest or query-consumer) so any app combining query-rest
 * with a binder-carrying module can pull this in, including a split rest/consumer deployment.
 * Ordered by name, not by class reference, since {@code QueryRestPushedCountsWebSocketAutoConfiguration}
 * isn't on this module's classpath.
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
