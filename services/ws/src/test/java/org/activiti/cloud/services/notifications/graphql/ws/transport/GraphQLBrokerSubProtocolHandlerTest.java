/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.ws.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;

import javax.websocket.Session;

import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerSubProtocolHandler;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLSessionConnectEvent;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLSessionDisconnectEvent;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLSessionSubscribeEvent;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLSessionUnsubscribeEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;


public class GraphQLBrokerSubProtocolHandlerTest {

    private GraphQLBrokerSubProtocolHandler testSubject;

    @Mock
    private MessageChannel outputChannel;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<Message<GraphQLMessage>> messageCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.testSubject = new GraphQLBrokerSubProtocolHandler("/ws/graphql");
        this.testSubject.setApplicationEventPublisher(applicationEventPublisher);

        when(outputChannel.send(Mockito.any(Message.class))).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        // nothing
    }

    @Test
    public void testGetSupportedProtocols() {
        assertThat(testSubject.getSupportedProtocols()).containsExactly("graphql-ws");
    }

    @Test
    public void testHandleConnectionInitMessageFromClient() throws Exception {
        // given
        TextMessage message = new TextMessage("{\"id\":\"1\",\"payload\":null,\"type\":\"connection_init\"}".getBytes());
        WebSocketSession session = mockWebSocketSession("sess1");

        // when
        testSubject.handleMessageFromClient(session, message, outputChannel);

        // then
        verify(outputChannel).send(messageCaptor.capture());
        verify(applicationEventPublisher).publishEvent(ArgumentMatchers.any(GraphQLSessionConnectEvent.class));

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("1");
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.CONNECTION_INIT);
    }

    @Test
    public void testHandleStartMessageFromClient() throws Exception {
        // given
        TextMessage message = new TextMessage("{\"id\":\"1\", \"payload\":{\"query\": \"{}\"}, \"type\":\"start\"}".getBytes());

        WebSocketSession session = mockWebSocketSession("sess1");

        // when
        testSubject.handleMessageFromClient(session, message, outputChannel);

        // then
        verify(outputChannel).send(messageCaptor.capture());
        verify(applicationEventPublisher).publishEvent(ArgumentMatchers.any(GraphQLSessionSubscribeEvent.class));

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("1");
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.START);
    }

    @Test
    public void testHandleStopMessageFromClient() throws Exception {
        // given
        TextMessage message = new TextMessage("{\"id\":\"1\", \"payload\":null, \"type\":\"stop\"}".getBytes());

        WebSocketSession session = mockWebSocketSession("sess1");

        // when
        testSubject.handleMessageFromClient(session, message, outputChannel);

        // then
        verify(outputChannel).send(messageCaptor.capture());
        verify(applicationEventPublisher).publishEvent(ArgumentMatchers.any(GraphQLSessionUnsubscribeEvent.class));

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("1");
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.STOP);
    }

    @Test
    public void testHandleConnectionTerminateMessageFromClient() throws Exception {
        // given
        TextMessage message = new TextMessage("{\"id\":\"1\", \"payload\":null, \"type\":\"connection_terminate\"}".getBytes());

        WebSocketSession session = mockWebSocketSession("sess1");

        // when
        testSubject.handleMessageFromClient(session, message, outputChannel);

        // then
        verify(outputChannel).send(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("1");
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.CONNECTION_TERMINATE);
    }

    @Test
    public void testHandleInvalidMessageToClient() throws IOException {
        // given
        WebSocketSession session = spy(mockWebSocketSession("sess1"));

        // when
        testSubject.handleMessageToClient(session, new GenericMessage<String>("Text Message"));

        // then
        verify(session, never()).sendMessage(ArgumentMatchers.any());
    }

    @Test
    public void testHandleConnectionAckMessageToClient() throws IOException {
        // given
        WebSocketSession session = spy(mockWebSocketSession("sess1"));

        doNothing().when(session).sendMessage(ArgumentMatchers.any(TextMessage.class));

        Message<GraphQLMessage> message = connectionAckMessage("operationId", session);

        // when
        testSubject.handleMessageToClient(session, message);

        // then
        verify(session).sendMessage(ArgumentMatchers.any(TextMessage.class));

    }

    @Test
    public void testHandleProtocolErrorMessageToClient() throws IOException {
        // given
        WebSocketSession session = spy(mockWebSocketSession("sess1"));
        doThrow(RuntimeException.class).when(session).sendMessage(ArgumentMatchers.any(TextMessage.class));

        Message<GraphQLMessage> message = connectionAckMessage("operationId", session);

        // when
        testSubject.handleMessageToClient(session, message);

        // then
        verify(session).close(ArgumentMatchers.eq(CloseStatus.PROTOCOL_ERROR));

    }

    @Test
    public void testAfterSessionEnded() throws Exception {
        // given
        WebSocketSession session = spy(mockWebSocketSession("sess1"));

        // when
        testSubject.afterSessionEnded(session, CloseStatus.BAD_DATA, outputChannel);

        // then
        verify(outputChannel).send(messageCaptor.capture());
        verify(applicationEventPublisher).publishEvent(ArgumentMatchers.any(GraphQLSessionDisconnectEvent.class));

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.CONNECTION_TERMINATE);
    }

    @Test
    public void testAfterSessionStarted() throws Exception {
        // given
        WebSocketSession session = spy(mockWebSocketSession("sess1"));
        when(session.getTextMessageSizeLimit()).thenReturn(1);

        // when
        testSubject.afterSessionStarted(session, outputChannel);

        // then
        verify(outputChannel, never()).send(messageCaptor.capture());
        verify(session).setTextMessageSizeLimit(ArgumentMatchers.eq(GraphQLBrokerSubProtocolHandler.MINIMUM_WEBSOCKET_MESSAGE_SIZE));
    }

    private WebSocketSession mockWebSocketSession(String sessionId) {
        Session nativeSession = mock(Session.class);
        when(nativeSession.getId()).thenReturn(sessionId);
        when(nativeSession.getUserPrincipal()).thenReturn(mock(Principal.class));

        StandardWebSocketSession wsSession = new StandardWebSocketSession(null,
                                                                          null,
                                                                          null,
                                                                          null);
        wsSession.initializeNativeSession(nativeSession);

        return wsSession;
    }

    private Message<GraphQLMessage> connectionAckMessage(String operationId, WebSocketSession session) {
        SimpMessageHeaderAccessor headerAccessor = simpHeaderAccessor(session);

        GraphQLMessage payload = new GraphQLMessage(operationId,
                                                    GraphQLMessageType.CONNECTION_ACK);

        return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
    }

    private SimpMessageHeaderAccessor simpHeaderAccessor(WebSocketSession session) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

        headerAccessor.setDestination("/destination");
        headerAccessor.setSessionId(session.getId());
        headerAccessor.setSessionAttributes(session.getAttributes());
        headerAccessor.setUser(session.getPrincipal());
        headerAccessor.setLeaveMutable(true);

        return headerAccessor;
    }

}
