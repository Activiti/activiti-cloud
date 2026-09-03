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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionInput;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalGroupsProviderChain;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.graphql.server.support.AuthenticationExtractor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PushedCountsWebSocketInterceptorTest {

    @Mock
    private AuthenticationExtractor authenticationExtractor;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthorizationManager<?> authorizationManager;

    @Mock
    private JwtPrincipalGroupsProviderChain principalGroupsProvider;

    @Mock
    private WebGraphQlInterceptor.Chain chain;

    @InjectMocks
    private PushedCountsWebSocketInterceptor interceptor;

    @Test
    void should_stashUserIdSessionIdAndGroups_when_requestIsFromAWebSocket() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");

        WebSocketSessionInfo sessionInfo = mock(WebSocketSessionInfo.class);
        when(sessionInfo.getId()).thenReturn("session-1");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
            PushedCountsWebSocketInterceptor.class.getName() + ".AUTHENTICATION",
            new SecurityContextImpl(authentication)
        );
        when(sessionInfo.getAttributes()).thenReturn(attributes);

        WebSocketGraphQlRequest request = mock(WebSocketGraphQlRequest.class);
        when(request.getSessionInfo()).thenReturn(sessionInfo);
        when(chain.next(request)).thenReturn(Mono.empty());

        interceptor.intercept(request, chain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BiFunction<ExecutionInput, ExecutionInput.Builder, ExecutionInput>> captor =
            ArgumentCaptor.forClass(BiFunction.class);
        verify(request).configureExecutionInput(captor.capture());

        ExecutionInput original = ExecutionInput.newExecutionInput("{}").build();
        ExecutionInput transformed = original.transform(builder -> captor.getValue().apply(original, builder));

        assertThat(
            transformed.getGraphQLContext().<String>get(PushedCountsWebSocketInterceptor.USER_ID_CONTEXT_KEY)
        ).isEqualTo("alice");
        assertThat(
            transformed.getGraphQLContext().<String>get(PushedCountsWebSocketInterceptor.SESSION_ID_CONTEXT_KEY)
        ).isEqualTo("session-1");
        assertThat(
            transformed.getGraphQLContext().<Set<String>>get(PushedCountsWebSocketInterceptor.GROUPS_CONTEXT_KEY)
        ).isEqualTo(Set.of());
    }

    @Test
    void should_fallBackToNoGroups_when_theGroupsProviderThrows() {
        Jwt jwt = mock(Jwt.class);
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt);
        GraphQLAccessToken accessToken = new GraphQLAccessToken("alice", Set.of(), jwtAuthenticationToken);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(authentication.getDetails()).thenReturn(accessToken);
        when(principalGroupsProvider.getGroups(jwtAuthenticationToken)).thenThrow(new RuntimeException("boom"));

        WebSocketSessionInfo sessionInfo = mock(WebSocketSessionInfo.class);
        when(sessionInfo.getId()).thenReturn("session-1");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
            PushedCountsWebSocketInterceptor.class.getName() + ".AUTHENTICATION",
            new SecurityContextImpl(authentication)
        );
        when(sessionInfo.getAttributes()).thenReturn(attributes);

        WebSocketGraphQlRequest request = mock(WebSocketGraphQlRequest.class);
        when(request.getSessionInfo()).thenReturn(sessionInfo);
        when(chain.next(request)).thenReturn(Mono.empty());

        interceptor.intercept(request, chain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BiFunction<ExecutionInput, ExecutionInput.Builder, ExecutionInput>> captor =
            ArgumentCaptor.forClass(BiFunction.class);
        verify(request).configureExecutionInput(captor.capture());

        ExecutionInput original = ExecutionInput.newExecutionInput("{}").build();
        ExecutionInput transformed = original.transform(builder -> captor.getValue().apply(original, builder));

        assertThat(
            transformed.getGraphQLContext().<Set<String>>get(PushedCountsWebSocketInterceptor.GROUPS_CONTEXT_KEY)
        ).isEqualTo(Set.of());
    }

    @Test
    void should_notStashAnything_when_requestIsNotFromAWebSocket() {
        WebGraphQlRequest request = mock(WebGraphQlRequest.class);
        when(chain.next(request)).thenReturn(Mono.empty());

        interceptor.intercept(request, chain);

        verify(request, never()).configureExecutionInput(any());
    }
}
