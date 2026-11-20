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
package org.activiti.cloud.services.notifications.graphql.ws.liveness;

import org.springframework.core.Ordered;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/**
 * Reports every inbound message and every closed session of the GraphQL WebSocket endpoint to the
 * {@link GraphQlWebSocketLivenessMonitor}.
 */
public class LivenessGraphQlWebSocketHandler extends WebSocketHandlerDecorator {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private final GraphQlWebSocketLivenessMonitor livenessMonitor;

    public LivenessGraphQlWebSocketHandler(WebSocketHandler delegate, GraphQlWebSocketLivenessMonitor livenessMonitor) {
        super(delegate);
        this.livenessMonitor = livenessMonitor;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        livenessMonitor.messageReceived(session);
        try {
            super.handleMessage(session, message);
        } finally {
            livenessMonitor.messageHandled(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        livenessMonitor.sessionClosed(session);
        super.afterConnectionClosed(session, closeStatus);
    }
}
