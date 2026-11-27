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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlCorsProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Replaces Spring Boot's GraphQL websocket handler with one that closes sessions when their JWT expires.
 */
@AutoConfiguration(
    afterName = {
        "org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration",
        "org.activiti.cloud.services.notifications.graphql.ws.config.GraphQLWebSocketMessageBrokerAutoConfiguration",
    },
    beforeName = "org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration"
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(
    value = { GraphQlWebSocketHandler.class, WebSocketHandler.class, ServerHttpMessageConvertersCustomizer.class },
    name = "jakarta.websocket.server.ServerContainer"
)
@ConditionalOnBean(ExecutionGraphQlService.class)
@ConditionalOnProperty(
    name = "spring.activiti.cloud.services.notification.graphql.ws.security.enabled",
    matchIfMissing = true
)
public class TokenExpiryGraphQlWebSocketAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GraphQlWebSocketHandler.class)
    public TokenExpiryGraphQlWebSocketHandler tokenExpiryGraphQlWebSocketHandler(
        WebGraphQlHandler webGraphQlHandler,
        GraphQlProperties properties,
        GraphQlCorsProperties corsProperties,
        ObjectProvider<ServerHttpMessageConvertersCustomizer> customizers
    ) {
        return new TokenExpiryGraphQlWebSocketHandler(
            webGraphQlHandler,
            jsonConverter(customizers),
            properties.getWebsocket().getConnectionInitTimeout(),
            properties.getWebsocket().getKeepAlive(),
            corsProperties.toCorsConfiguration()
        );
    }

    @SuppressWarnings("unchecked")
    private static HttpMessageConverter<Object> jsonConverter(
        ObjectProvider<ServerHttpMessageConvertersCustomizer> customizers
    ) {
        HttpMessageConverters.ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
        customizers.forEach(customizer -> customizer.customize(serverBuilder));
        for (HttpMessageConverter<?> converter : serverBuilder.build()) {
            if (converter.canRead(Map.class, MediaType.APPLICATION_JSON)) {
                return (HttpMessageConverter<Object>) converter;
            }
        }
        throw new IllegalStateException("No JSON converter");
    }
}
