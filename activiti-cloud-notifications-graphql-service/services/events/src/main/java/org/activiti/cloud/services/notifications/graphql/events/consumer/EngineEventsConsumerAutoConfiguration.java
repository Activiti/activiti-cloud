/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.notifications.graphql.events.consumer;

import java.util.Arrays;
import java.util.List;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.services.notifications.graphql.events.RoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.SpELTemplateRoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.events.transformer.EngineEventsTransformer;
import org.activiti.cloud.services.notifications.graphql.events.transformer.Transformer;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Notification Gateway configuration that enables messaging channel bindings
 * and scans for MessagingGateway on interfaces to create GatewayProxyFactoryBeans.
 */
@Configuration
@EnableConfigurationProperties(EngineEventsConsumerProperties.class)
@ConditionalOnProperty(name = "spring.activiti.cloud.services.notifications.graphql.events.enabled", matchIfMissing = true)
@PropertySources({
    @PropertySource(value = "classpath:META-INF/graphql-events.properties"),
    @PropertySource(value = "classpath:graphql-events.properties", ignoreResourceNotFound = true)
})
public class EngineEventsConsumerAutoConfiguration {

    @Configuration
    public static class DefaultEngineEventsConsumerConfiguration  implements EngineEventsConsumerChannels {

        public static final String ENGINE_EVENTS_FLUX_SCHEDULER = "engineEventsScheduler";
        private final EngineEventsConsumerProperties properties;

        @Autowired
        public DefaultEngineEventsConsumerConfiguration(EngineEventsConsumerProperties properties) {
            this.properties = properties;
        }

        @Bean
        @ConditionalOnMissingBean
        public RoutingKeyResolver routingKeyResolver() {
            return new SpELTemplateRoutingKeyResolver();
        }

        @InputBinding(SOURCE)
        @Override
        public SubscribableChannel input() {
            return MessageChannels.publishSubscribe(SOURCE).get();
        }

        @Bean
        @ConditionalOnMissingBean
        public Transformer engineEventsTransformer() {
            return new EngineEventsTransformer(Arrays.asList(properties.getProcessEngineEventAttributeKeys()
                                                                       .split(",")),
                                               properties.getProcessEngineEventTypeKey());
        }

        @Bean
        @ConditionalOnMissingBean
        public EngineEventsConsumerMessageHandler engineEventsMessageHandler(Transformer engineEventsTransformer) {
            return new EngineEventsConsumerMessageHandler(engineEventsTransformer);
        }

        @Bean
        @ConditionalOnMissingBean
        public Publisher<Message<List<EngineEvent>>> engineEventsPublisher(EngineEventsConsumerMessageHandler engineEventsMessageHandler,
                @Qualifier(SOURCE) SubscribableChannel source) {

            return IntegrationFlows.from(source)
                                   .log(LoggingHandler.Level.DEBUG)
                                   .transform(engineEventsMessageHandler)
                                   .toReactivePublisher();
        }

        @Bean
        @ConditionalOnMissingBean
        public Flux<Message<List<EngineEvent>>> engineEventsFlux(Publisher<Message<List<EngineEvent>>> engineEventsPublisher,
                                                                 Scheduler engineEventsScheduler) {
            return Flux.from(engineEventsPublisher)
                       .publish()
                       .autoConnect(0)
                       .share()
                       .publishOn(engineEventsScheduler);
        }

        @Bean
        @ConditionalOnMissingBean(name = ENGINE_EVENTS_FLUX_SCHEDULER)
        public Scheduler engineEventsScheduler() {
            return Schedulers.boundedElastic();
        }
    }
}
