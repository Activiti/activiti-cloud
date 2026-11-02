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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class TokenExpiryGraphQlWebSocketHandlerTest {

    private static final TextMessage PING = new TextMessage("{\"type\":\"ping\"}");

    private TokenExpiryGraphQlWebSocketHandler handler;
    private WebSocketSession session;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        WebGraphQlHandler webGraphQlHandler = mock(WebGraphQlHandler.class);
        when(webGraphQlHandler.contextSnapshotFactory()).thenReturn(ContextSnapshotFactory.builder().build());
        handler = new TokenExpiryGraphQlWebSocketHandler(
            webGraphQlHandler,
            new JacksonJsonHttpMessageConverter(),
            Duration.ofMinutes(1),
            null,
            null
        );

        attributes = new HashMap<>();
        attributes.put(ContextSnapshot.class.getName(), ContextSnapshotFactory.builder().build().captureAll());
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
    }

    @Test
    void should_closeWithUnauthorized_when_messageArrivesAfterTokenExpiry() throws Exception {
        attributes.put(SecurityWebSocketInterceptor.TOKEN_EXPIRES_AT_ATTRIBUTE, Instant.now().minusSeconds(1));

        handler.handleMessage(session, PING);

        verify(session).close(TokenExpiryGraphQlWebSocketHandler.TOKEN_EXPIRED_STATUS);
        verify(session, never()).sendMessage(any());
    }

    @Test
    void should_closeWithUnauthorized_when_tokenExpiresWhileConnectionIsOpen() throws Exception {
        attributes.put(SecurityWebSocketInterceptor.TOKEN_EXPIRES_AT_ATTRIBUTE, Instant.now().plusMillis(300));

        handler.handleMessage(session, PING);

        verify(session, timeout(5000)).close(TokenExpiryGraphQlWebSocketHandler.TOKEN_EXPIRED_STATUS);
    }

    @Test
    void should_notClose_when_connectionClosesBeforeTokenExpiry() throws Exception {
        attributes.put(SecurityWebSocketInterceptor.TOKEN_EXPIRES_AT_ATTRIBUTE, Instant.now().plusMillis(300));

        handler.handleMessage(session, PING);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, after(1000).never()).close(any());
    }
}
