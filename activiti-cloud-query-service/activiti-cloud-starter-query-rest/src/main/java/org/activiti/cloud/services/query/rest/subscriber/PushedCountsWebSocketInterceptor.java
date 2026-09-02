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

import org.activiti.cloud.services.notifications.qraphql.ws.security.SecurityWebSocketInterceptor;
import org.springframework.graphql.server.support.AuthenticationExtractor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationManager;

/**
 * Websocket interceptor for the pushed-counts feature - currently just a distinctly-named
 * {@link SecurityWebSocketInterceptor}, proving the "one WebSocketGraphQlInterceptor per
 * application" composition works in combined deployments before behavior is added.
 *
 * <p>Registration with {@link SubscriberRegistry} can't happen at connection time: on a
 * connection shared with an unrelated subscription (e.g. {@code engineEvents}), that would
 * register every client of that other feature too. A later step wires registration to an actual
 * pushed-counts subscription instead.
 */
public class PushedCountsWebSocketInterceptor extends SecurityWebSocketInterceptor {

    public PushedCountsWebSocketInterceptor(
        AuthenticationExtractor authenticationExtractor,
        AuthenticationManager authenticationManager,
        AuthorizationManager<?> authorizationManager
    ) {
        super(authenticationExtractor, authenticationManager, authorizationManager);
    }
}
