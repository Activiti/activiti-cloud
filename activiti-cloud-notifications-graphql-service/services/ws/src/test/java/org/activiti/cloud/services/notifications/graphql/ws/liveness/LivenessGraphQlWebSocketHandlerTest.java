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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

class LivenessGraphQlWebSocketHandlerTest {

    private static final TextMessage PONG = new TextMessage("{\"type\":\"pong\"}");

    private GraphQlWebSocketLivenessMonitor monitor;
    private WebSocketHandler delegate;
    private WebSocketSession session;
    private LivenessGraphQlWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        monitor = mock(GraphQlWebSocketLivenessMonitor.class);
        delegate = mock(WebSocketHandler.class);
        session = mock(WebSocketSession.class);
        handler = new LivenessGraphQlWebSocketHandler(delegate, monitor);
        when(monitor.messageReceived(session)).thenReturn(true);
    }

    @Test
    void should_reportMessageAroundDelegate_when_messageReceived() throws Exception {
        handler.handleMessage(session, PONG);

        InOrder inOrder = inOrder(monitor, delegate);
        inOrder.verify(monitor).messageReceived(session);
        inOrder.verify(delegate).handleMessage(session, PONG);
        inOrder.verify(monitor).messageHandled(session);
    }

    @Test
    void should_reportMessageHandled_when_delegateFails() throws Exception {
        doThrow(new IllegalStateException("boom")).when(delegate).handleMessage(session, PONG);

        assertThatThrownBy(() -> handler.handleMessage(session, PONG)).isInstanceOf(IllegalStateException.class);
        verify(monitor).messageHandled(session);
    }

    @Test
    void should_dropMessage_when_monitorRejectsItBecauseSessionIsClosing() throws Exception {
        when(monitor.messageReceived(session)).thenReturn(false);

        handler.handleMessage(session, PONG);

        verify(delegate, never()).handleMessage(any(), any());
        verify(monitor, never()).messageHandled(session);
    }

    @Test
    void should_reportClosedSession_when_connectionClosed() throws Exception {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        InOrder inOrder = inOrder(monitor, delegate);
        inOrder.verify(monitor).sessionClosed(session);
        inOrder.verify(delegate).afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
