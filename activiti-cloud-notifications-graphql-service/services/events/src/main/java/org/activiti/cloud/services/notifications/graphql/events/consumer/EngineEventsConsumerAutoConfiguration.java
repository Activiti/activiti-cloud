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
package org.activiti.cloud.services.notifications.graphql.events.consumer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.notifications.graphql.events.RoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.SpELTemplateRoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.events.transformer.EngineEventsTransformer;
import org.activiti.cloud.services.notifications.graphql.events.transformer.Transformer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Notification Gateway configuration that enables messaging channel bindings
 * and scans for MessagingGateway on interfaces to create GatewayProxyFactoryBeans.
 */
@AutoConfiguration
@EnableConfigurationProperties(EngineEventsConsumerProperties.class)
@ConditionalOnProperty(
    name = "spring.activiti.cloud.services.notifications.graphql.events.enabled",
    matchIfMissing = true
)
@PropertySources(
    {
        @PropertySource(value = "classpath:META-INF/graphql-events.properties"),
        @PropertySource(value = "classpath:graphql-events.properties", ignoreResourceNotFound = true),
    }
)
public class EngineEventsConsumerAutoConfiguration {

    @Configuration
    public static class DefaultEngineEventsConsumerConfiguration implements EngineEventsConsumerChannels {

        private static final Logger logger = LoggerFactory.getLogger(DefaultEngineEventsConsumerConfiguration.class);

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

        @Bean
        @ConditionalOnMissingBean
        public Transformer engineEventsTransformer() {
            return new EngineEventsTransformer(
                Arrays.asList(properties.getProcessEngineEventAttributeKeys().split(",")),
                properties.getProcessEngineEventTypeKey()
            );
        }

        @Bean
        @ConditionalOnMissingBean
        public EngineEventsConsumerMessageHandler engineEventsMessageHandler(Transformer engineEventsTransformer) {
            return new EngineEventsConsumerMessageHandler(engineEventsTransformer);
        }

        @Bean
        @FunctionBinding(input = SOURCE)
        public Consumer<Message<List<EngineEvent>>> engineEventsGraphQlSourceConsumer(
            MessageChannel engineEventsPublisherInput
        ) {
            return engineEventsPublisherInput::send;
        }

        @Bean
        MessageChannel engineEventsPublisherInput() {
            return MessageChannels.direct("engineEventsPublisherInput").getObject();
        }

        @Bean
        @ConditionalOnMissingBean
        public Publisher<Message<List<EngineEvent>>> engineEventsPublisher(
            EngineEventsConsumerMessageHandler engineEventsMessageHandler,
            MessageChannel engineEventsPublisherInput
        ) {
            return IntegrationFlow
                .from(engineEventsPublisherInput)
                .log(LoggingHandler.Level.DEBUG)
                .gateway(
                    gatewayFlow -> gatewayFlow.transform(engineEventsMessageHandler),
                    gatewaySpec -> gatewaySpec.sendTimeout(-1).requestTimeout(-1L).requiresReply(false)
                )
                .toReactivePublisher();
        }

        @Bean
        @ConditionalOnMissingBean
        public Flux<Message<List<EngineEvent>>> engineEventsFlux(
            Publisher<Message<List<EngineEvent>>> engineEventsPublisher,
            Scheduler engineEventsScheduler
        ) {
            return Flux
                .from(engineEventsPublisher)
                .doOnError(error -> logger.error("Error while publishing engine events: {}", error.getMessage(), error))
                .onErrorResume(e -> Mono.empty())
                .publish()
                .autoConnect()
                .parallel()
                .runOn(engineEventsScheduler)
                .sequential()
                .onBackpressureLatest();
        }

        @Bean
        @ConditionalOnMissingBean(name = ENGINE_EVENTS_FLUX_SCHEDULER)
        public Scheduler engineEventsScheduler() {
            return Schedulers.boundedElastic();
        }

        @Bean
        InitializingBean engineEventsFluxConsumer(Flux<Message<List<EngineEvent>>> engineEventsFlux) {
            return () -> {
                logger.info("Subscribing engineEventsFlux consumer");

                engineEventsFlux.subscribe(
                    message -> logger.debug("Received engine events {}", message.getHeaders()),
                    e -> logger.error("Error while receiving engine events", e),
                    () -> logger.warn("Completing engineEventsFlux consumer")
                );
            };
        }
    }
}
