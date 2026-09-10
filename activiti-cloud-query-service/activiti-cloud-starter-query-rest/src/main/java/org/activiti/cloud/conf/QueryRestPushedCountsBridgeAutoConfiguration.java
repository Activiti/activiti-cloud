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

import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistryBroadcaster;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberResyncResponder;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

/**
 * Wires the query-rest producer side of the pushed-counts subscriber registry: broadcasts this
 * instance's REGISTERED / UNREGISTERED / HEARTBEAT / SNAPSHOT onto the shared, group-less registry
 * destination and answers the consumer's RESYNC_REQUEST with a SNAPSHOT. Every message is stamped
 * with a stable per-instance {@code sourceId} (an explicit {@code instance-id}, e.g. the pod
 * hostname, or a random UUID).
 *
 * <p>Kept separate from {@link QueryRestPushedCountsWebSocketAutoConfiguration} because it needs a
 * Spring Cloud Stream binder at runtime, which the bare REST starter does not carry:
 * {@code @ConditionalOnClass(StreamBridge.class)} keeps it inert unless a stream-capable module is
 * present. Gated at startup by {@code activiti.cloud.query.pushed-counts.enabled} (off by default,
 * turned on in hxp-process-services) so that toggle off means no beans and therefore no traffic.
 * Presence tracking is intentionally independent of the {@code activiti.features.query.pushed-counts.enabled}
 * runtime toggle, which gates the count push (later steps), not who is watching.
 */
@AutoConfiguration(after = QueryRestPushedCountsWebSocketAutoConfiguration.class)
@ConditionalOnClass(StreamBridge.class)
@ConditionalOnProperty(
    name = "activiti.cloud.query.pushed-counts.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@PropertySource("classpath:pushed-counts-bridge.properties")
@EnableScheduling
public class QueryRestPushedCountsBridgeAutoConfiguration {

    private final String sourceId;

    public QueryRestPushedCountsBridgeAutoConfiguration(
        @Value("${activiti.cloud.query.pushed-counts.instance-id:}") String configuredInstanceId
    ) {
        this.sourceId = StringUtils.hasText(configuredInstanceId) ? configuredInstanceId : UUID.randomUUID().toString();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscriberRegistryBroadcaster subscriberRegistryBroadcaster(
        StreamBridge streamBridge,
        SubscriberRegistry subscriberRegistry,
        Clock pushedCountsClock,
        @Value("${ACT_QUERY_SUBSCRIBER_REGISTRY_DEST:subscriberRegistry}") String registryDestination
    ) {
        return new SubscriberRegistryBroadcaster(
            streamBridge,
            subscriberRegistry,
            sourceId,
            registryDestination,
            pushedCountsClock
        );
    }

    /**
     * The consumer of RESYNC_REQUEST; the method name is the function definition bound in
     * {@code pushed-counts-bridge.properties}.
     */
    @Bean
    public Consumer<Message<SubscriberRegistryMessage>> subscriberRegistryResyncResponder(
        StreamBridge streamBridge,
        SubscriberRegistry subscriberRegistry,
        Clock pushedCountsClock,
        @Value("${ACT_QUERY_SUBSCRIBER_REGISTRY_DEST:subscriberRegistry}") String registryDestination
    ) {
        return new SubscriberResyncResponder(
            subscriberRegistry,
            streamBridge,
            sourceId,
            registryDestination,
            pushedCountsClock
        );
    }
}
