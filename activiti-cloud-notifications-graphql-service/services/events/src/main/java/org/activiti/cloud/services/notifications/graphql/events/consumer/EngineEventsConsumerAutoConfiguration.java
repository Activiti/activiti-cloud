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

import org.activiti.cloud.services.notifications.graphql.events.RoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.SpELTemplateRoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.events.transformer.EngineEventsTransformer;
import org.activiti.cloud.services.notifications.graphql.events.transformer.Transformer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Notification Gateway configuration that enables messaging channel bindings
 * and scans for MessagingGateway on interfaces to create GatewayProxyFactoryBeans.
 */
@Configuration
@EnableBinding(EngineEventsConsumerChannels.class)
@EnableConfigurationProperties(EngineEventsConsumerProperties.class)
@ConditionalOnProperty(name = "spring.activiti.cloud.services.notifications.graphql.events.enabled", matchIfMissing = true)
@PropertySources({
    @PropertySource(value = "classpath:META-INF/graphql-events.properties"),
    @PropertySource(value = "classpath:graphql-events.properties", ignoreResourceNotFound = true)
})
public class EngineEventsConsumerAutoConfiguration {

    @Configuration
    public static class DefaultEngineEventsConsumerConfiguration {

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
        public Publisher<Message<List<EngineEvent>>> engineEventsPublisher(EngineEventsConsumerMessageHandler engineEventsMessageHandler) {
            return IntegrationFlows.from(EngineEventsConsumerChannels.SOURCE)
                                   .log(LoggingHandler.Level.DEBUG)
                                   .transform(engineEventsMessageHandler)
                                   .toReactivePublisher();
        }

        @Bean
        @ConditionalOnMissingBean
        public Flux<Message<List<EngineEvent>>> engineEventsFlux(Publisher<Message<List<EngineEvent>>> engineEventsPublisher) {
            return Flux.from(engineEventsPublisher)
                       .publish()
                       .autoConnect();
        }
    }

    //@Configuration
    public static class EngineEventsFluxProcessorConfiguration implements SmartLifecycle {
        private static Logger logger = LoggerFactory.getLogger(EngineEventsFluxProcessorConfiguration.class);

        private Optional<Disposable> control = Optional.empty();

        private final ConnectableFlux<Message<List<EngineEvent>>> engineEventsFlux;

        @Autowired
        public EngineEventsFluxProcessorConfiguration(ConnectableFlux<Message<List<EngineEvent>>> engineEventsFlux) {
            this.engineEventsFlux = engineEventsFlux;
        }

        @Override
        public void start() {
            if (control.isEmpty()) {
                logger.info("Connect engineEvents to the message event source");
                control = Optional.of(engineEventsFlux.connect());
            }
        }

        @Override
        public void stop() {
            control.ifPresent(disposable -> {
                logger.info("Dispose of engineEvents stream");
                disposable.dispose();
            });
        }

        @Override
        public boolean isRunning() {
            return control.isPresent();
        }
    }

}
