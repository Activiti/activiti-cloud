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

import graphql.GraphQL;
import java.time.Clock;
import java.time.Duration;
import org.activiti.cloud.services.notifications.qraphql.ws.security.JWSAuthenticationManager;
import org.activiti.cloud.services.notifications.qraphql.ws.security.JWSBearerTokenAuthenticationExtractor;
import org.activiti.cloud.services.notifications.qraphql.ws.security.WebSocketMessageBrokerSecurityAutoConfiguration;
import org.activiti.cloud.services.query.rest.subscriber.PushedCountsWebSocketInterceptor;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberSessionExpirySweep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Wires the pushed-counts websocket connection interceptor ({@link PushedCountsWebSocketInterceptor}),
 * the in-memory {@link SubscriberRegistry}, and the session expiry backstop
 * ({@link SubscriberSessionExpirySweep}). The interceptor and the registry are not yet wired to
 * each other - see {@link PushedCountsWebSocketInterceptor}'s javadoc for why.
 *
 * <p>Ordered before {@link WebSocketMessageBrokerSecurityAutoConfiguration} so its own
 * {@code authenticationInterceptor} bean sees this one already registered and backs off,
 * since spring-graphql allows at most one {@code WebSocketGraphQlInterceptor}.
 */
@AutoConfiguration(before = WebSocketMessageBrokerSecurityAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnClass({ GraphQL.class, WebSocketGraphQlInterceptor.class })
@EnableScheduling
public class QueryRestPushedCountsWebSocketAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock pushedCountsClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscriberRegistry subscriberRegistry(
        ApplicationEventPublisher eventPublisher,
        @Value("${query.pushed-counts.registry.max-size:50000}") long maxSize
    ) {
        return new SubscriberRegistry(eventPublisher, maxSize);
    }

    @Bean
    @ConditionalOnMissingBean(WebSocketGraphQlInterceptor.class)
    public PushedCountsWebSocketInterceptor pushedCountsWebSocketInterceptor(
        JWSBearerTokenAuthenticationExtractor jwsBearerTokenAuthenticationExtractor,
        JWSAuthenticationManager jwsAuthenticationManager,
        AuthorizationManager<RequestAuthorizationContext> graphQlWebSocketAuthorizationManager
    ) {
        return new PushedCountsWebSocketInterceptor(
            jwsBearerTokenAuthenticationExtractor,
            jwsAuthenticationManager,
            graphQlWebSocketAuthorizationManager
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscriberSessionExpirySweep subscriberSessionExpirySweep(
        SubscriberRegistry subscriberRegistry,
        Clock pushedCountsClock,
        @Value("${query.pushed-counts.session.expiry:PT5M}") Duration sessionExpiry
    ) {
        return new SubscriberSessionExpirySweep(subscriberRegistry, pushedCountsClock, sessionExpiry);
    }
}
