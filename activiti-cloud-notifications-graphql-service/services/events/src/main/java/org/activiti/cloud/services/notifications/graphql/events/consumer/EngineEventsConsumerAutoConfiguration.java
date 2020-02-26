/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.events.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.activiti.cloud.services.notifications.graphql.events.RoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.SpELTemplateRoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.events.transformer.EngineEventsTransformer;
import org.activiti.cloud.services.notifications.graphql.events.transformer.Transformer;
import org.reactivestreams.Subscriber;
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
import org.springframework.messaging.Message;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.TopicProcessor;

/**
 * Notification Gateway configuration that enables messaging channel bindings
 * and scans for MessagingGateway on interfaces to create GatewayProxyFactoryBeans.
 *
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
        public EngineEventsConsumerMessageHandler engineEventsMessageHandler(Transformer engineEventsTransformer,
                                                                             FluxSink<Message<List<EngineEvent>>> engineEventsSink) {
            return new EngineEventsConsumerMessageHandler(engineEventsTransformer, engineEventsSink);
        }

    }
    
    @Configuration
    public static class EngineEventsFluxProcessorConfiguration implements SmartLifecycle {

        private final List<Subscriber<Message<List<EngineEvent>>>> subscribers = new ArrayList<>();
        private boolean running;

        private TopicProcessor<Message<List<EngineEvent>>> engineEventsProcessor = TopicProcessor.<Message<List<EngineEvent>>>builder()
                                                                                                 .autoCancel(false)
                                                                                                 .share(true)
                                                                                                 .bufferSize(1024)
                                                                                                 .build();
        @Autowired
        public EngineEventsFluxProcessorConfiguration() {
        }

        @Autowired(required = false)
        public void setSubscribers(List<Subscriber<Message<List<EngineEvent>>>> subscribers) {
            this.subscribers.addAll(subscribers);
        }

        @Bean
        @ConditionalOnMissingBean
        public Flux<Message<List<EngineEvent>>> engineEventsFlux() {
            return engineEventsProcessor.publish()
                                        .autoConnect(0);
        }

        @Bean
        @ConditionalOnMissingBean
        public FluxSink<Message<List<EngineEvent>>> engineEventsSink() {
            return engineEventsProcessor.sink();
        }

        @Override
        public void start() {
            subscribers.forEach(s -> engineEventsProcessor.subscribe(s));
            running = true;
        }

        @Override
        public void stop() {
            try {
                engineEventsProcessor.onComplete();
            } finally {
                running = false;
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }
    
}
