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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * Closes GraphQL websocket sessions when the access token provided at connection initialization expires.
 */
public class TokenExpiryGraphQlWebSocketHandler extends GraphQlWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenExpiryGraphQlWebSocketHandler.class);

    static final CloseStatus TOKEN_EXPIRED_STATUS = new CloseStatus(4403, "Forbidden");

    private final Map<String, Disposable> expiryTimers = new ConcurrentHashMap<>();

    public TokenExpiryGraphQlWebSocketHandler(
        WebGraphQlHandler graphQlHandler,
        HttpMessageConverter<Object> converter,
        Duration connectionInitTimeout,
        Duration keepAliveDuration,
        CorsConfiguration corsConfiguration
    ) {
        super(graphQlHandler, converter, connectionInitTimeout, keepAliveDuration, corsConfiguration);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (isTokenExpired(session)) {
            closeExpiredSession(session);
            return;
        }
        super.handleTextMessage(session, message);
        scheduleCloseOnTokenExpiry(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        Disposable timer = expiryTimers.remove(session.getId());
        if (timer != null) {
            timer.dispose();
        }
        super.afterConnectionClosed(session, closeStatus);
    }

    private void scheduleCloseOnTokenExpiry(WebSocketSession session) {
        Instant expiresAt = tokenExpiresAt(session);
        if (expiresAt == null || !session.isOpen()) {
            return;
        }
        String sessionId = session.getId();
        expiryTimers.computeIfAbsent(sessionId, id -> {
            Duration delay = Duration.between(Instant.now(), expiresAt);
            return Mono.delay(delay.isNegative() ? Duration.ZERO : delay).subscribe(tick -> {
                expiryTimers.remove(id);
                closeExpiredSession(session);
            });
        });
    }

    private static boolean isTokenExpired(WebSocketSession session) {
        Instant expiresAt = tokenExpiresAt(session);
        return expiresAt != null && !Instant.now().isBefore(expiresAt);
    }

    private static Instant tokenExpiresAt(WebSocketSession session) {
        return session.getAttributes().get(SecurityWebSocketInterceptor.TOKEN_EXPIRES_AT_ATTRIBUTE) instanceof
            Instant expiresAt
            ? expiresAt
            : null;
    }

    private static void closeExpiredSession(WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }
        LOGGER.debug("Closing GraphQL websocket session {}: access token expired", session.getId());
        try {
            session.close(TOKEN_EXPIRED_STATUS);
        } catch (IOException e) {
            LOGGER.debug("Error while closing expired GraphQL websocket session {}", session.getId(), e);
        }
    }
}
