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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys.Badge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PushedCountDataFetcherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private SubscriberRegistry subscriberRegistry;

    private final Sinks.Many<CountChangedMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Flux<CountChangedMessage> pushedCountsFlux = sink.asFlux();
    private final PushedCountsSubscriptionTracker tracker = new PushedCountsSubscriptionTracker();

    private DataFetchingEnvironment environmentFor(String userId, String sessionId, Set<String> groups) {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        Map<Object, Object> context = new HashMap<>();
        if (userId != null) {
            context.put(PushedCountsWebSocketInterceptor.USER_ID_CONTEXT_KEY, userId);
        }
        if (sessionId != null) {
            context.put(PushedCountsWebSocketInterceptor.SESSION_ID_CONTEXT_KEY, sessionId);
        }
        context.put(PushedCountsWebSocketInterceptor.GROUPS_CONTEXT_KEY, groups);
        when(environment.getGraphQlContext()).thenReturn(GraphQLContext.of(context));
        return environment;
    }

    @Test
    void should_deliverTheCount_when_theScopeKeyMatchesTheBadgeAndUserId() {
        PushedCountDataFetcher fetcher = new PushedCountDataFetcher(
            Badge.ASSIGNED,
            pushedCountsFlux,
            subscriberRegistry,
            tracker,
            CLOCK
        );
        Publisher<PushedCount> publisher = fetcher.get(environmentFor("alice", "session-1", Set.of("eng")));

        StepVerifier.create(Flux.from(publisher))
            .then(() -> sink.tryEmitNext(new CountChangedMessage("assigned:alice", 3, NOW)))
            .expectNext(new PushedCount(3, NOW.toString()))
            .thenCancel()
            .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = { "queued:alice", "assigned:bob", "not-a-scope-key" })
    void should_notDeliverTheCount_when_theScopeKeyDoesNotMatch(String scopeKey) {
        PushedCountDataFetcher fetcher = new PushedCountDataFetcher(
            Badge.ASSIGNED,
            pushedCountsFlux,
            subscriberRegistry,
            tracker,
            CLOCK
        );
        Publisher<PushedCount> publisher = fetcher.get(environmentFor("alice", "session-1", Set.of("eng")));

        StepVerifier.create(Flux.from(publisher))
            .then(() -> sink.tryEmitNext(new CountChangedMessage(scopeKey, 3, NOW)))
            .expectNoEvent(Duration.ofMillis(200))
            .thenCancel()
            .verify();
    }

    @Test
    void should_returnAnEmptyFlux_when_userIdIsMissingFromTheContext() {
        PushedCountDataFetcher fetcher = new PushedCountDataFetcher(
            Badge.ASSIGNED,
            pushedCountsFlux,
            subscriberRegistry,
            tracker,
            CLOCK
        );
        Publisher<PushedCount> publisher = fetcher.get(environmentFor(null, "session-1", Set.of("eng")));

        StepVerifier.create(Flux.from(publisher)).expectComplete().verify();
    }

    @Test
    void should_returnAnEmptyFlux_when_sessionIdIsMissingFromTheContext() {
        PushedCountDataFetcher fetcher = new PushedCountDataFetcher(
            Badge.ASSIGNED,
            pushedCountsFlux,
            subscriberRegistry,
            tracker,
            CLOCK
        );
        Publisher<PushedCount> publisher = fetcher.get(environmentFor("alice", null, Set.of("eng")));

        StepVerifier.create(Flux.from(publisher)).expectComplete().verify();
    }

    @Test
    void should_registerOnce_when_twoBadgesAreSubscribedOnTheSameSession() {
        PushedCountDataFetcher myTasks = new PushedCountDataFetcher(
            Badge.ASSIGNED,
            pushedCountsFlux,
            subscriberRegistry,
            tracker,
            CLOCK
        );
        PushedCountDataFetcher queuedTasks = new PushedCountDataFetcher(
            Badge.QUEUED,
            pushedCountsFlux,
            subscriberRegistry,
            tracker,
            CLOCK
        );

        var myTasksDisposable = Flux.from(myTasks.get(environmentFor("alice", "session-1", Set.of("eng")))).subscribe();
        var queuedTasksDisposable = Flux.from(
            queuedTasks.get(environmentFor("alice", "session-1", Set.of("eng")))
        ).subscribe();

        verify(subscriberRegistry).register("alice", Set.of("eng"), "session-1", NOW);

        myTasksDisposable.dispose();
        verify(subscriberRegistry, never()).unregister(any(), any(), any());

        queuedTasksDisposable.dispose();
        verify(subscriberRegistry).unregister("alice", "session-1", NOW);
    }
}
