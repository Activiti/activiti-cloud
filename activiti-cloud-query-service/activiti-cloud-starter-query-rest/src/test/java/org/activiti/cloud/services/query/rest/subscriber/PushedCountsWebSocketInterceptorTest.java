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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalGroupsProviderChain;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PushedCountsWebSocketInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @Mock
    private FeatureToggle featureToggle;

    @Mock
    private JwtPrincipalGroupsProviderChain principalGroupsProvider;

    @Mock
    private SubscriberRegistry subscriberRegistry;

    private PushedCountsWebSocketInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new PushedCountsWebSocketInterceptor(
            jwtDecoder,
            jwtAccessTokenValidator,
            jwt -> new TestingAuthenticationToken(jwt.getSubject(), null, List.of()),
            featureToggle,
            principalGroupsProvider,
            subscriberRegistry,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void should_refuseTheConnection_when_thePushedCountsFeatureToggleIsDisabled() {
        when(featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)).thenReturn(false);

        Mono<Object> result = interceptor.handleConnectionInitialization(
            new FakeWebSocketSessionInfo("session-1"),
            new HashMap<>()
        );

        assertThatExceptionOfType(AuthenticationException.class).isThrownBy(result::block);
        verifyNoInteractions(subscriberRegistry);
    }

    @Test
    void should_registerTheSubscriber_when_theConnectionAuthenticatesSuccessfully() {
        when(featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)).thenReturn(true);
        Jwt jwt = validJwtFor("alice");
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        lenient().when(jwtAccessTokenValidator.isValid(jwt)).thenReturn(true);
        when(principalGroupsProvider.getGroups(any(Authentication.class))).thenReturn(List.of("eng"));
        Map<String, Object> payload = new HashMap<>();
        payload.put("Authorization", "bearer valid-token");
        FakeWebSocketSessionInfo sessionInfo = new FakeWebSocketSessionInfo("session-1");

        Mono<Object> result = interceptor.handleConnectionInitialization(sessionInfo, payload);

        result.block();
        verify(subscriberRegistry).register(eq("alice"), any(), eq("session-1"), eq(NOW));
    }

    @Test
    void should_notRegister_when_theTokenFailsValidation() {
        when(featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)).thenReturn(true);
        Jwt jwt = validJwtFor("alice");
        when(jwtDecoder.decode("invalid-token")).thenReturn(jwt);
        when(jwtAccessTokenValidator.isValid(jwt)).thenReturn(false);
        Map<String, Object> payload = new HashMap<>();
        payload.put("Authorization", "bearer invalid-token");

        Mono<Object> result = interceptor.handleConnectionInitialization(
            new FakeWebSocketSessionInfo("session-1"),
            payload
        );

        assertThatExceptionOfType(AuthenticationException.class).isThrownBy(result::block);
        verifyNoInteractions(subscriberRegistry);
    }

    @Test
    void should_unregisterTheSubscriber_when_theConnectionCloses() {
        when(featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)).thenReturn(true);
        Jwt jwt = validJwtFor("alice");
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        lenient().when(jwtAccessTokenValidator.isValid(jwt)).thenReturn(true);
        when(principalGroupsProvider.getGroups(any(Authentication.class))).thenReturn(List.of());
        Map<String, Object> payload = new HashMap<>();
        payload.put("Authorization", "bearer valid-token");
        FakeWebSocketSessionInfo sessionInfo = new FakeWebSocketSessionInfo("session-1");
        interceptor.handleConnectionInitialization(sessionInfo, payload).block();

        interceptor.handleConnectionClosed(sessionInfo, 1000, payload);

        verify(subscriberRegistry).unregister("alice", "session-1", NOW);
    }

    private static Jwt validJwtFor(String subject) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(subject)
            .claim("sub", subject)
            .issuedAt(NOW)
            .expiresAt(NOW.plusSeconds(3600))
            .build();
    }

    private record FakeWebSocketSessionInfo(String id, Map<String, Object> attributes) implements WebSocketSessionInfo {
        FakeWebSocketSessionInfo(String id) {
            this(id, new HashMap<>());
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws/pushed-counts");
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }

        @Override
        public Mono<Principal> getPrincipal() {
            return Mono.empty();
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }
    }
}
