/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.graphql.server.support.AbstractAuthenticationWebSocketInterceptor;
import org.springframework.graphql.server.support.AuthenticationExtractor;
import org.springframework.graphql.server.support.BearerTokenAuthenticationExtractor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class AuthenticationWebSocketInterceptorImpl extends AbstractAuthenticationWebSocketInterceptor {

    private final AuthenticationManager authenticationManager;

    public AuthenticationWebSocketInterceptorImpl(
        AuthenticationExtractor authExtractor,
        AuthenticationManager authenticationManager
    ) {
        super(authExtractor);
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Mono<Object> handleConnectionInitialization(WebSocketSessionInfo info, Map<String, Object> payload) {
        // Workaround for missing Authorization header in the payload
        if (!payload.containsKey(BearerTokenAuthenticationExtractor.AUTHORIZATION_KEY)) {
            payload.put(BearerTokenAuthenticationExtractor.AUTHORIZATION_KEY, "");
        }
        return super.handleConnectionInitialization(info, payload);
    }

    @Override
    protected Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(this.authenticationManager.authenticate(authentication));
    }

    @Override
    protected ContextView getContextToWrite(SecurityContext securityContext) {
        String key = SecurityContext.class.getName(); // match SecurityContextThreadLocalAccessor key
        return Context.of(key, securityContext);
    }
}
