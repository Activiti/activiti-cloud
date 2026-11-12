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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.graphql.server.support.AuthenticationExtractor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class SecurityWebSocketInterceptorTest {

    @Mock
    private AuthenticationExtractor authenticationExtractor;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthorizationManager<?> authorizationManager;

    @InjectMocks
    private SecurityWebSocketInterceptor interceptor;

    @Test
    void should_recordTokenExpiry_when_connectionIsInitialized() {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("alice")
            .issuedAt(Instant.now())
            .expiresAt(expiresAt)
            .build();
        GraphQLAccessToken accessToken = new GraphQLAccessToken("alice", Set.of(), new JwtAuthenticationToken(jwt));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getDetails()).thenReturn(accessToken);
        Authentication unauthenticated = mock(Authentication.class);
        when(authenticationExtractor.getAuthentication(any())).thenReturn(Mono.just(unauthenticated));
        when(authenticationManager.authenticate(unauthenticated)).thenReturn(authentication);

        Map<String, Object> attributes = new HashMap<>();
        WebSocketSessionInfo sessionInfo = mock(WebSocketSessionInfo.class);
        when(sessionInfo.getAttributes()).thenReturn(attributes);

        interceptor.handleConnectionInitialization(sessionInfo, new HashMap<>()).block();

        assertThat(attributes).containsEntry(SecurityWebSocketInterceptor.TOKEN_EXPIRES_AT_ATTRIBUTE, expiresAt);
    }

    @Test
    void should_notRecordTokenExpiry_when_authenticationHasNoToken() {
        Authentication authentication = mock(Authentication.class);
        Authentication unauthenticated = mock(Authentication.class);
        when(authenticationExtractor.getAuthentication(any())).thenReturn(Mono.just(unauthenticated));
        when(authenticationManager.authenticate(unauthenticated)).thenReturn(authentication);

        Map<String, Object> attributes = new HashMap<>();
        WebSocketSessionInfo sessionInfo = mock(WebSocketSessionInfo.class);
        when(sessionInfo.getAttributes()).thenReturn(attributes);

        interceptor.handleConnectionInitialization(sessionInfo, new HashMap<>()).block();

        assertThat(attributes).doesNotContainKey(SecurityWebSocketInterceptor.TOKEN_EXPIRES_AT_ATTRIBUTE);
    }
}
