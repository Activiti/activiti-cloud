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
package org.activiti.cloud.services.notifications.graphql.ws.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.activiti.cloud.services.notifications.graphql.ws.handler.DecoratingGraphQlWebSocketHandler;
import org.activiti.cloud.services.notifications.graphql.ws.handler.GraphQlWebSocketHandlerDecoratorFactory;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessMonitor;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessProperties;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.LivenessGraphQlWebSocketHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.GraphQlCorsProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import reactor.core.scheduler.Schedulers;

/**
 * Replaces Spring Boot's GraphQL WebSocket handler with a {@link DecoratingGraphQlWebSocketHandler}, built from the
 * same Boot settings as in {@link GraphQlWebMvcAutoConfiguration}, so that features such as the liveness check can
 * decorate the endpoint independently of each other.
 */
@AutoConfiguration(
    after = { GraphQlAutoConfiguration.class, GraphQLWebSocketMessageBrokerAutoConfiguration.class },
    before = GraphQlWebMvcAutoConfiguration.class
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(
    value = { GraphQlWebSocketHandler.class, WebSocketHandler.class, ServerHttpMessageConvertersCustomizer.class },
    name = "jakarta.websocket.server.ServerContainer"
)
@ConditionalOnBean(ExecutionGraphQlService.class)
@ConditionalOnProperty(name = "spring.activiti.cloud.services.query.graphql.ws.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GraphQlCorsProperties.class)
public class GraphQlWebSocketHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GraphQlWebSocketHandler.class)
    public DecoratingGraphQlWebSocketHandler decoratingGraphQlWebSocketHandler(
        WebGraphQlHandler webGraphQlHandler,
        GraphQlProperties properties,
        GraphQlCorsProperties corsProperties,
        ObjectProvider<ServerHttpMessageConvertersCustomizer> customizers,
        ObjectProvider<GraphQlWebSocketHandlerDecoratorFactory> decoratorFactories
    ) {
        CorsConfiguration corsConfiguration = corsProperties.toCorsConfiguration();
        return new DecoratingGraphQlWebSocketHandler(
            webGraphQlHandler,
            jsonConverter(customizers),
            properties.getWebsocket().getConnectionInitTimeout(),
            properties.getWebsocket().getKeepAlive(),
            corsConfiguration != null ? corsConfiguration : new CorsConfiguration(),
            decoratorFactories.orderedStream().toList()
        );
    }

    @SuppressWarnings("unchecked")
    private static HttpMessageConverter<Object> jsonConverter(
        ObjectProvider<ServerHttpMessageConvertersCustomizer> customizers
    ) {
        HttpMessageConverters.ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
        customizers.orderedStream().forEach(customizer -> customizer.customize(serverBuilder));
        for (HttpMessageConverter<?> converter : serverBuilder.build()) {
            if (converter.canRead(Map.class, MediaType.APPLICATION_JSON)) {
                return (HttpMessageConverter<Object>) converter;
            }
        }
        throw new IllegalStateException("No JSON converter");
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = GraphQlWebSocketLivenessProperties.PREFIX + ".enabled", matchIfMissing = true)
    @EnableConfigurationProperties(GraphQlWebSocketLivenessProperties.class)
    static class LivenessConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public GraphQlWebSocketLivenessMonitor graphQlWebSocketLivenessMonitor(
            GraphQlWebSocketLivenessProperties livenessProperties,
            GraphQlProperties graphQlProperties
        ) {
            Duration timeout = livenessProperties.getTimeout();
            Duration keepAlive = graphQlProperties.getWebsocket().getKeepAlive();
            if (keepAlive == null) {
                throw new IllegalStateException(
                    "GraphQL WebSocket liveness check requires spring.graphql.websocket.keep-alive to be set, " +
                        "or set " +
                        GraphQlWebSocketLivenessProperties.PREFIX +
                        ".enabled=false"
                );
            }
            if (timeout.compareTo(keepAlive) <= 0) {
                throw new IllegalStateException(
                    GraphQlWebSocketLivenessProperties.PREFIX +
                        ".timeout (" +
                        timeout +
                        ") must be longer than spring.graphql.websocket.keep-alive (" +
                        keepAlive +
                        ")"
                );
            }
            return new GraphQlWebSocketLivenessMonitor(
                timeout,
                livenessProperties.getCheckInterval(),
                Clock.systemUTC(),
                Schedulers.boundedElastic()::schedule
            );
        }

        @Bean
        @Order(LivenessGraphQlWebSocketHandler.ORDER)
        public GraphQlWebSocketHandlerDecoratorFactory livenessGraphQlWebSocketHandlerDecoratorFactory(
            GraphQlWebSocketLivenessMonitor graphQlWebSocketLivenessMonitor
        ) {
            return handler -> new LivenessGraphQlWebSocketHandler(handler, graphQlWebSocketLivenessMonitor);
        }
    }
}
