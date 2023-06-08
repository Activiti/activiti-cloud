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
package org.activiti.cloud.services.notifications.graphql.ws.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import graphql.ExecutionResultImpl;
import jakarta.websocket.Session;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscription;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

@ExtendWith(MockitoExtension.class)
public class GraphQLBrokerChannelSubscriberTest {

    private GraphQLBrokerChannelSubscriber testSubject;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private Subscription subscription;

    @Captor
    private ArgumentCaptor<Message<GraphQLMessage>> messageCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        Message<GraphQLMessage> startMessage = startMessage("operationId");

        this.testSubject = new GraphQLBrokerChannelSubscriber(startMessage, "operationId", messageChannel, 1000, 1);
    }

    @Test
    public void testCancel() {
        // given
        testOnSubscribe();

        // when
        testSubject.cancel();

        // then
        verify(subscription).cancel();
    }

    @Test
    public void testOnSubscribe() {
        // given

        // when
        testSubject.onSubscribe(subscription);

        // then
        verify(subscription).request(eq(1L));
    }

    @Test
    public void testOnNext() {
        // given
        testOnSubscribe();

        // when
        testSubject.onNext(new ExecutionResultImpl(Collections.singletonMap("key", "value"), Collections.emptyList()));

        // then
        verify(messageChannel).send(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.DATA);
    }

    @Test
    public void testOnError() {
        // given
        testOnSubscribe();

        // when
        testSubject.onError(new RuntimeException());

        // then
        verify(messageChannel).send(messageCaptor.capture());
        verifyNoMoreInteractions(messageChannel, subscription);

        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.ERROR);
    }

    @Test
    public void testOnComplete() {
        // given
        testOnSubscribe();

        // when
        testSubject.onComplete();

        // then
        verify(messageChannel).send(messageCaptor.capture());
        verify(subscription).cancel();

        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.COMPLETE);
    }

    private Message<GraphQLMessage> startMessage(String operationId) {
        SimpMessageHeaderAccessor headerAccessor = simpHeaderAccessor(mockWebSocketSession());

        Map<String, Object> json = new HashMap<>();
        json.put("query", "{}");
        json.put("variables", "{}");

        GraphQLMessage payload = new GraphQLMessage(operationId, GraphQLMessageType.START, json);

        return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
    }

    private WebSocketSession mockWebSocketSession() {
        Session nativeSession = mock(Session.class);
        when(nativeSession.getUserPrincipal()).thenReturn(mock(Principal.class));

        StandardWebSocketSession wsSession = new StandardWebSocketSession(null, null, null, null);
        wsSession.initializeNativeSession(nativeSession);

        return wsSession;
    }

    private SimpMessageHeaderAccessor simpHeaderAccessor(WebSocketSession session) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

        headerAccessor.setDestination("/destination");
        headerAccessor.setSessionAttributes(session.getAttributes());
        headerAccessor.setUser(session.getPrincipal());
        headerAccessor.setLeaveMutable(true);

        return headerAccessor;
    }
}
