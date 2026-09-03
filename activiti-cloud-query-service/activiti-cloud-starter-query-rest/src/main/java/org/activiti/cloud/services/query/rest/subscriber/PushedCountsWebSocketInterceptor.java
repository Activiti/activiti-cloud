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

import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalGroupsProviderChain;
import org.activiti.cloud.services.notifications.qraphql.ws.security.SecurityWebSocketInterceptor;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.graphql.server.support.AuthenticationExtractor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

/**
 * Websocket interceptor for the pushed-counts feature - a {@link SecurityWebSocketInterceptor}
 * that additionally stashes the connection's authenticated user id, groups, and session id into
 * the {@code GraphQLContext} of every request, since none of those are otherwise reachable from
 * a {@link PushedCountDataFetcher}.
 */
public class PushedCountsWebSocketInterceptor extends SecurityWebSocketInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushedCountsWebSocketInterceptor.class);

    static final String USER_ID_CONTEXT_KEY = PushedCountsWebSocketInterceptor.class.getName() + ".USER_ID";
    static final String GROUPS_CONTEXT_KEY = PushedCountsWebSocketInterceptor.class.getName() + ".GROUPS";
    static final String SESSION_ID_CONTEXT_KEY = PushedCountsWebSocketInterceptor.class.getName() + ".SESSION_ID";

    private final JwtPrincipalGroupsProviderChain principalGroupsProvider;

    public PushedCountsWebSocketInterceptor(
        AuthenticationExtractor authenticationExtractor,
        AuthenticationManager authenticationManager,
        AuthorizationManager<?> authorizationManager,
        JwtPrincipalGroupsProviderChain principalGroupsProvider
    ) {
        super(authenticationExtractor, authenticationManager, authorizationManager);
        this.principalGroupsProvider = principalGroupsProvider;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (request instanceof WebSocketGraphQlRequest webSocketRequest) {
            stashConnectionContext(request, webSocketRequest.getSessionInfo());
        }
        return super.intercept(request, chain);
    }

    private void stashConnectionContext(WebGraphQlRequest request, WebSocketSessionInfo sessionInfo) {
        Authentication authentication = currentAuthentication(sessionInfo);
        if (authentication == null) {
            return;
        }
        String userId = authentication.getName();
        Set<String> groups = resolveGroups(authentication);
        String sessionId = sessionInfo.getId();
        request.configureExecutionInput((executionInput, builder) ->
            builder
                .graphQLContext(context -> {
                    context.put(USER_ID_CONTEXT_KEY, userId);
                    context.put(GROUPS_CONTEXT_KEY, groups);
                    context.put(SESSION_ID_CONTEXT_KEY, sessionId);
                })
                .build()
        );
    }

    private Set<String> resolveGroups(Authentication authentication) {
        if (
            !(authentication.getDetails() instanceof GraphQLAccessToken accessToken) ||
            !(accessToken.getDetails() instanceof JwtAuthenticationToken jwtAuthenticationToken)
        ) {
            return Set.of();
        }
        // A groups lookup failure must not take the websocket connection down - fall back to no groups.
        try {
            List<String> groups = principalGroupsProvider.getGroups(jwtAuthenticationToken);
            return groups != null ? Set.copyOf(groups) : Set.of();
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to resolve groups for {}; treating as no groups", authentication.getName(), e);
            return Set.of();
        }
    }
}
