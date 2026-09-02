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
 * The websocket connection interceptor for the pushed-counts feature - currently a distinctly
 * named, otherwise-unmodified {@link SecurityWebSocketInterceptor}.
 *
 * <p>It adds no behavior of its own yet: registering a connection with {@link SubscriberRegistry}
 * cannot happen here, because the connection opening does not mean the client wants counts - on a
 * connection shared with an unrelated GraphQL subscription (e.g. notifications-graphql-service's
 * {@code engineEvents}), that would register every client of that other feature too. Registration
 * has to be triggered by a client actually subscribing to a pushed-counts badge, which does not
 * exist until a later step adds a real {@code Subscription} field and a data fetcher to hook into.
 *
 * <p>This class exists now, as its own bean, so that spring-graphql's "at most one
 * {@code WebSocketGraphQlInterceptor}" composition - this bean winning over
 * {@code WebSocketMessageBrokerSecurityAutoConfiguration}'s default - is already proven to work in
 * a combined deployment before that later step adds real behavior to it.
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
