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
import graphql.schema.idl.TypeRuntimeWiring;
import java.time.Clock;
import java.time.Duration;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalGroupsProviderChain;
import org.activiti.cloud.services.notifications.qraphql.ws.security.JWSAuthenticationManager;
import org.activiti.cloud.services.notifications.qraphql.ws.security.JWSBearerTokenAuthenticationExtractor;
import org.activiti.cloud.services.notifications.qraphql.ws.security.WebSocketMessageBrokerSecurityAutoConfiguration;
import org.activiti.cloud.services.query.rest.subscriber.PushedCountDataFetcher;
import org.activiti.cloud.services.query.rest.subscriber.PushedCountsSubscriptionTracker;
import org.activiti.cloud.services.query.rest.subscriber.PushedCountsWebSocketInterceptor;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberSessionExpirySweep;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys.Badge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Wires the pushed-counts websocket interceptor, in-memory {@link SubscriberRegistry}, session
 * expiry sweep, and the relay that fans a {@link CountChangedMessage} out to the three badge
 * subscriptions via {@link PushedCountDataFetcher}. Ordered before
 * {@link WebSocketMessageBrokerSecurityAutoConfiguration} so its default interceptor bean backs
 * off, since spring-graphql allows only one {@code WebSocketGraphQlInterceptor}.
 *
 * <p>The {@code Sinks.Many<CountChangedMessage>} bean is not wired to any messaging channel here:
 * that needs a real Spring Cloud Stream binder at boot, which this bare REST starter does not
 * carry - added instead by whichever starter combines it with a messaging-capable module.
 */
@AutoConfiguration(before = WebSocketMessageBrokerSecurityAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnClass({ GraphQL.class, WebSocketGraphQlInterceptor.class })
@EnableScheduling
public class QueryRestPushedCountsWebSocketAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryRestPushedCountsWebSocketAutoConfiguration.class);

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
        AuthorizationManager<RequestAuthorizationContext> graphQlWebSocketAuthorizationManager,
        JwtPrincipalGroupsProviderChain principalGroupsProvider
    ) {
        return new PushedCountsWebSocketInterceptor(
            jwsBearerTokenAuthenticationExtractor,
            jwsAuthenticationManager,
            graphQlWebSocketAuthorizationManager,
            principalGroupsProvider
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

    @Bean
    @ConditionalOnMissingBean
    public PushedCountsSubscriptionTracker pushedCountsSubscriptionTracker() {
        return new PushedCountsSubscriptionTracker();
    }

    @Bean
    @ConditionalOnMissingBean
    public Sinks.Many<CountChangedMessage> pushedCountsSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    @ConditionalOnMissingBean
    public Flux<CountChangedMessage> pushedCountsFlux(Sinks.Many<CountChangedMessage> pushedCountsSink) {
        return pushedCountsSink.asFlux().publish().autoConnect().onBackpressureLatest();
    }

    @Bean
    InitializingBean pushedCountsFluxConsumer(Flux<CountChangedMessage> pushedCountsFlux) {
        return () ->
            pushedCountsFlux.subscribe(
                message -> LOGGER.debug("Received pushed count {}", message),
                error -> LOGGER.error("Error while receiving pushed counts", error),
                () -> LOGGER.warn("Completing pushedCountsFlux consumer")
            );
    }

    @Bean
    public RuntimeWiringConfigurer pushedCountsRuntimeWiringConfigurer(
        Flux<CountChangedMessage> pushedCountsFlux,
        SubscriberRegistry subscriberRegistry,
        PushedCountsSubscriptionTracker pushedCountsSubscriptionTracker,
        Clock pushedCountsClock
    ) {
        return builder ->
            builder.type(
                TypeRuntimeWiring.newTypeWiring("Subscription")
                    .dataFetcher(
                        "assignedTasks",
                        new PushedCountDataFetcher(
                            Badge.ASSIGNED,
                            pushedCountsFlux,
                            subscriberRegistry,
                            pushedCountsSubscriptionTracker,
                            pushedCountsClock
                        )
                    )
                    .dataFetcher(
                        "queuedTasks",
                        new PushedCountDataFetcher(
                            Badge.QUEUED,
                            pushedCountsFlux,
                            subscriberRegistry,
                            pushedCountsSubscriptionTracker,
                            pushedCountsClock
                        )
                    )
                    .dataFetcher(
                        "runningProcesses",
                        new PushedCountDataFetcher(
                            Badge.PROCESSES,
                            pushedCountsFlux,
                            subscriberRegistry,
                            pushedCountsSubscriptionTracker,
                            pushedCountsClock
                        )
                    )
            );
    }
}
