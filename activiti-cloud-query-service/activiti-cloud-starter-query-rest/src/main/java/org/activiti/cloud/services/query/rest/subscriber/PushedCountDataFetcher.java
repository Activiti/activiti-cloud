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
package org.activiti.cloud.services.query.rest.subscriber;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Clock;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys;
import org.activiti.cloud.services.query.subscription.ScopeKeys.Badge;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Backs one pushed-counts badge subscription field (e.g. {@code myTasks} for
 * {@link Badge#ASSIGNED}). Registers/unregisters with {@link SubscriberRegistry} via
 * {@link PushedCountsSubscriptionTracker} on this {@code Flux}'s first subscribe / last cancel -
 * spring-graphql cancels it for us on unsubscribe or disconnect, so no separate handling is needed.
 */
public class PushedCountDataFetcher implements DataFetcher<Publisher<PushedCount>> {

    private final Badge badge;
    private final Flux<CountChangedMessage> pushedCountsFlux;
    private final SubscriberRegistry subscriberRegistry;
    private final PushedCountsSubscriptionTracker subscriptionTracker;
    private final Clock clock;

    public PushedCountDataFetcher(
        Badge badge,
        Flux<CountChangedMessage> pushedCountsFlux,
        SubscriberRegistry subscriberRegistry,
        PushedCountsSubscriptionTracker subscriptionTracker,
        Clock clock
    ) {
        this.badge = badge;
        this.pushedCountsFlux = pushedCountsFlux;
        this.subscriberRegistry = subscriberRegistry;
        this.subscriptionTracker = subscriptionTracker;
        this.clock = clock;
    }

    @Override
    public Publisher<PushedCount> get(DataFetchingEnvironment environment) {
        GraphQLContext context = environment.getGraphQlContext();
        String userId = context.get(PushedCountsWebSocketInterceptor.USER_ID_CONTEXT_KEY);
        String sessionId = context.get(PushedCountsWebSocketInterceptor.SESSION_ID_CONTEXT_KEY);
        Set<String> groups = context.get(PushedCountsWebSocketInterceptor.GROUPS_CONTEXT_KEY);
        if (userId == null || sessionId == null) {
            return Flux.empty();
        }
        return pushedCountsFlux
            .filter(message -> matches(message, userId))
            .map(message -> new PushedCount(Math.toIntExact(message.count()), message.asOf().toString()))
            .doOnSubscribe(subscription -> {
                if (subscriptionTracker.incrementAndCheckIfWasZero(sessionId)) {
                    subscriberRegistry.register(userId, groups, sessionId, clock.instant());
                }
            })
            .doFinally(signalType -> {
                if (subscriptionTracker.decrementAndCheckIfNowZero(sessionId)) {
                    subscriberRegistry.unregister(userId, sessionId, clock.instant());
                }
            });
    }

    /** A scope key this feature doesn't recognize is treated as "not for me", never as an error. */
    private boolean matches(CountChangedMessage message, String userId) {
        try {
            ScopeKeys.ScopeKey scopeKey = ScopeKeys.parse(message.scopeKey());
            return scopeKey.badge() == badge && scopeKey.userId().equals(userId);
        } catch (IllegalArgumentException _) {
            return false;
        }
    }
}
