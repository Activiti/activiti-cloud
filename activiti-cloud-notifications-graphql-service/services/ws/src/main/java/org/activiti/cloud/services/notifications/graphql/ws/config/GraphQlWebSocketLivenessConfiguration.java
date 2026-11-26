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

import java.time.Duration;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessMonitor;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessProperties;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.LivenessGraphQlWebSocketHandlerDecoratorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(GraphQlWebSocketHandlerAutoConfiguration.WEBSOCKET_PATH_PROPERTY)
@ConditionalOnProperty(name = GraphQlWebSocketLivenessProperties.PREFIX + ".enabled", matchIfMissing = true)
@EnableConfigurationProperties(GraphQlWebSocketLivenessProperties.class)
class GraphQlWebSocketLivenessConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GraphQlWebSocketLivenessMonitor graphQlWebSocketLivenessMonitor(
        GraphQlWebSocketLivenessProperties livenessProperties,
        GraphQlProperties graphQlProperties
    ) {
        Duration timeout = requirePositive("timeout", livenessProperties.getTimeout());
        Duration checkInterval = requirePositive("check-interval", livenessProperties.getCheckInterval());
        if (checkInterval.compareTo(timeout) >= 0) {
            throw new IllegalStateException(
                GraphQlWebSocketLivenessProperties.PREFIX +
                    ".check-interval (" +
                    checkInterval +
                    ") must be shorter than " +
                    GraphQlWebSocketLivenessProperties.PREFIX +
                    ".timeout (" +
                    timeout +
                    ")"
            );
        }
        Duration keepAlive = graphQlProperties.getWebsocket().getKeepAlive();
        if (keepAlive == null) {
            throw new IllegalStateException(
                "GraphQL WebSocket liveness check requires spring.graphql.websocket.keep-alive to be set, " +
                    "or set " +
                    GraphQlWebSocketLivenessProperties.PREFIX +
                    ".enabled=false"
            );
        }
        if (!keepAlive.isPositive() || keepAlive.compareTo(timeout) >= 0) {
            throw new IllegalStateException(
                "spring.graphql.websocket.keep-alive (" +
                    keepAlive +
                    ") must be positive and shorter than " +
                    GraphQlWebSocketLivenessProperties.PREFIX +
                    ".timeout (" +
                    timeout +
                    ")"
            );
        }
        return new GraphQlWebSocketLivenessMonitor(
            timeout,
            checkInterval,
            System::nanoTime,
            Schedulers.boundedElastic()::schedule
        );
    }

    private static Duration requirePositive(String property, Duration value) {
        if (!value.isPositive()) {
            throw new IllegalStateException(
                GraphQlWebSocketLivenessProperties.PREFIX + "." + property + " must be positive, but was " + value
            );
        }
        return value;
    }

    @Bean
    public LivenessGraphQlWebSocketHandlerDecoratorFactory livenessGraphQlWebSocketHandlerDecoratorFactory(
        GraphQlWebSocketLivenessMonitor graphQlWebSocketLivenessMonitor
    ) {
        return new LivenessGraphQlWebSocketHandlerDecoratorFactory(graphQlWebSocketLivenessMonitor);
    }
}
