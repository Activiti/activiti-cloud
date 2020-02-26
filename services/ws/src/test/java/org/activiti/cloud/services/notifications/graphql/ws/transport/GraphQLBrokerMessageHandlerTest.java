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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerChannelSubscriber;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerMessageHandler;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerSubscriptionRegistry;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLSubscriptionExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class GraphQLBrokerMessageHandlerTest {

    private final static String destination = "/ws/graphql";

    private GraphQLBrokerMessageHandler messageHandler;

    @Mock
    private SubscribableChannel clientInboundChannel;

    @Mock
    private ExecutorSubscribableChannel clientOutboundChannel;

    @Mock
    private SubscribableChannel brokerChannel;

    @Mock
    private GraphQLSubscriptionExecutor graphQLExecutor;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture scheduledFuture;

    @Captor
    private ArgumentCaptor<Message<GraphQLMessage>> messageCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.messageHandler = new GraphQLBrokerMessageHandler(this.clientInboundChannel,
                                                              this.clientOutboundChannel,
                                                              this.brokerChannel,
                                                              graphQLExecutor);

        this.messageHandler.setTaskScheduler(taskScheduler);
        when(taskScheduler.scheduleWithFixedDelay(Mockito.any(Runnable.class), Mockito.anyLong())).thenReturn(
                                                                                                              scheduledFuture);

        this.messageHandler.start();

        // when
        this.messageHandler.on(new BrokerAvailabilityEvent(true, this));

        // then
        assertThat(this.messageHandler.isBrokerAvailable()).isTrue();
    }

    @After
    public void tearDown() throws Exception {
        //
    }

    @Test
    public void testStartStop() {
        assertThat(this.messageHandler.isRunning()).isTrue();
        this.messageHandler.stop();
        assertThat(this.messageHandler.isRunning()).isFalse();

        this.messageHandler.start();
        assertThat(this.messageHandler.isRunning()).isTrue();
    }

    @Test
    public void testBrokerNonAvailabilityEvent() {
        // when
        this.messageHandler.on(new BrokerAvailabilityEvent(false, this));

        // then
        assertThat(this.messageHandler.isBrokerAvailable()).isFalse();
    }

    @Test
    public void testStarInternal() {
        verify(taskScheduler).scheduleWithFixedDelay(Mockito.any(Runnable.class), Mockito.anyLong());
    }

    @Test
    public void testStopInternal() {
        // when
        this.messageHandler.stop();

        // then
        verify(scheduledFuture).cancel(Mockito.anyBoolean());
    }

    @Test
    public void testHandleConnectionInitMessageBrokerAvailableSendsConnectionAck() {
        // when
        this.messageHandler.handleMessage(connectionInitMessage("id", "sess1"));

        // then
        verify(this.clientOutboundChannel).send(this.messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.CONNECTION_ACK);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("id");
    }

    @Test
    public void testHandleConnectionInitMessageBrokerUnavailableSendsConnectionError() {
        // given
        this.messageHandler.on(new BrokerAvailabilityEvent(false, this));

        // when
        this.messageHandler.handleMessage(connectionInitMessage("id", "sess1"));

        // then
        verify(this.clientOutboundChannel).send(this.messageCaptor.capture());

        assertThat(messageCaptor.getValue()
                                .getPayload()).isInstanceOf(GraphQLMessage.class);

        assertThat(messageCaptor.getValue()
                                .getPayload()
                                .getType()).isEqualTo(GraphQLMessageType.CONNECTION_ERROR);

        assertThat(messageCaptor.getValue()
                                .getPayload()
                                .getId()).isEqualTo("id");

        assertThat(messageCaptor.getValue()
                                .getPayload()
                                .getPayload()).containsEntry("errors",
                                                             Collections.singletonList(GraphQLBrokerMessageHandler.BROKER_NOT_AVAILABLE));
    }

    @Test
    public void testHandleStartMessageBrokerAvailableSendsData() throws InterruptedException {
        // given
        Message<GraphQLMessage> message = startMessage("operationId", "sess1");
        CountDownLatch completeLatch = new CountDownLatch(1);

        // Simulate stomp relay  subscription stream
        Flux<ExecutionResult> mockStompRelayObservable = Flux.interval(Duration.ZERO, Duration.ofMillis(20))
                                                             .take(100)                   
                                                             .map(i -> {
                                                                 Map<String, Object> data = new HashMap<>();
                                                                 data.put("key", i);

                                                                 return new ExecutionResultImpl(data,
                                                                                                Collections.emptyList());
                                                             });

        StepVerifier observable = StepVerifier.create(mockStompRelayObservable)
                .expectNextCount(100)
                .expectComplete();
        
        ExecutionResult executionResult = stubExecutionResult(mockStompRelayObservable, completeLatch);

        when(graphQLExecutor.execute(Mockito.anyString(), Mockito.any()))
                                                                         .thenReturn(executionResult);

        // when
        this.messageHandler.handleMessage(message);

        observable.verify(Duration.ofMinutes(2));

        assertThat(completeLatch.await(2000, TimeUnit.MILLISECONDS)).isTrue();
        
        // then get last message
        verify(this.clientOutboundChannel, atLeast(99)).send(this.messageCaptor.capture());

        GraphQLMessage completeMessage = messageCaptor.getValue().getPayload();

        assertThat(completeMessage.getType()).isEqualTo(GraphQLMessageType.COMPLETE);
    }

    @Test
    public void testHandleStartMessageBrokerAvailableDataNullSendError() {
        // given
        Message<GraphQLMessage> message = startMessage("id", "sess1");

        ExecutionResult executionResult = mock(ExecutionResult.class);
        when(graphQLExecutor.execute(Mockito.anyString(), Mockito.any())).thenReturn(executionResult);
        when(executionResult.getErrors()).thenReturn(Collections.emptyList());
        when(executionResult.getData()).thenReturn(null);

        // when
        this.messageHandler.handleMessage(message);

        // then
        verify(this.clientOutboundChannel).send(this.messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.ERROR);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("id");
        assertThat(messageCaptor.getValue().getPayload().getPayload()).containsKey("errors");
    }

    @Test
    public void testHandleStartMessageBrokerAvailableExecutorErrorSendsError() {
        // given
        Message<GraphQLMessage> message = startMessage("id", "sess1");

        ExecutionResult executionResult = mock(ExecutionResult.class);
        when(graphQLExecutor.execute(Mockito.anyString(), Mockito.any())).thenReturn(executionResult);
        when(executionResult.getErrors()).thenReturn(Collections.singletonList(mock(GraphQLError.class)));
        when(executionResult.getData()).thenReturn(null);

        // when
        this.messageHandler.handleMessage(message);

        // then
        verify(this.clientOutboundChannel).send(this.messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPayload()).isInstanceOf(GraphQLMessage.class);
        assertThat(messageCaptor.getValue().getPayload().getType()).isEqualTo(GraphQLMessageType.ERROR);
        assertThat(messageCaptor.getValue().getPayload().getId()).isEqualTo("id");
        assertThat(messageCaptor.getValue().getPayload().getPayload()).containsKey("errors");
    }

    @Test
    public void testHandleStartMessageBrokerUnavailableSendsError() {
        // given
        this.messageHandler.on(new BrokerAvailabilityEvent(false, this));
        Message<GraphQLMessage> message = startMessage("id", "sess1");

        // when
        this.messageHandler.handleMessage(message);

        // then
        verify(this.clientOutboundChannel).send(this.messageCaptor.capture());

        assertThat(messageCaptor.getValue()
                                .getPayload()).isInstanceOf(GraphQLMessage.class);

        assertThat(messageCaptor.getValue()
                                .getPayload()
                                .getType()).isEqualTo(GraphQLMessageType.ERROR);

        assertThat(messageCaptor.getValue()
                                .getPayload()
                                .getId()).isEqualTo("id");

        assertThat(messageCaptor.getValue()
                                .getPayload()
                                .getPayload()).containsKey("errors");
    }

    @Test
    public void testHandleStopMessageCompletesSubscriber() {
        // given
        Message<GraphQLMessage> message = stopMessage("subscriptionId", "sessionId");

        GraphQLBrokerChannelSubscriber subscriber = mock(GraphQLBrokerChannelSubscriber.class);
        GraphQLBrokerSubscriptionRegistry registry = messageHandler.getGraphQLsubscriptionRegistry();
        registry.subscribe("sessionId", "subscriptionId", subscriber);

        // when
        this.messageHandler.handleMessage(message);

        // then
        verify(subscriber).onComplete();

    }

    @Test
    public void testHandleConnectionTerminateMessageCancelsSubscriber() {
        // given
        WebSocketSession session = mockWebSocketSession("sessionId");
        Message<GraphQLMessage> message = createDisconnectMessage(session);

        GraphQLBrokerChannelSubscriber subscriber = mock(GraphQLBrokerChannelSubscriber.class);
        GraphQLBrokerSubscriptionRegistry registry = messageHandler.getGraphQLsubscriptionRegistry();
        registry.subscribe("sessionId", "subscriptionId", subscriber);

        // when
        this.messageHandler.handleMessage(message);

        // then
        verify(subscriber).cancel();

    }

    private Message<GraphQLMessage> connectionInitMessage(String operationId, String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = simpHeaderAccessor(mockWebSocketSession(sessionId));

        headerAccessor.setHeader(StompHeaderAccessor.HEART_BEAT_HEADER, new long[]{0, 5000});

        GraphQLMessage payload = new GraphQLMessage(operationId,
                                                    GraphQLMessageType.CONNECTION_INIT);

        return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
    }

    private Message<GraphQLMessage> startMessage(String operationId, String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = simpHeaderAccessor(mockWebSocketSession(sessionId));

        Map<String, Object> json = new HashMap<>();
        json.put("query", "{}");
        json.put("variables", "{}");

        GraphQLMessage payload = new GraphQLMessage(operationId,
                                                    GraphQLMessageType.START,
                                                    json);

        return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
    }

    private Message<GraphQLMessage> stopMessage(String operationId, String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = simpHeaderAccessor(mockWebSocketSession(sessionId));

        GraphQLMessage payload = new GraphQLMessage(operationId,
                                                    GraphQLMessageType.STOP);

        return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
    }

    private Message<GraphQLMessage> createDisconnectMessage(WebSocketSession session) {
        SimpMessageHeaderAccessor headerAccessor = simpHeaderAccessor(session);

        GraphQLMessage payload = new GraphQLMessage(null,
                                                    GraphQLMessageType.CONNECTION_TERMINATE,
                                                    Collections.emptyMap());

        return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
    }

    private SimpMessageHeaderAccessor simpHeaderAccessor(WebSocketSession session) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

        headerAccessor.setDestination(destination);
        headerAccessor.setSessionId(session.getId());
        headerAccessor.setSessionAttributes(session.getAttributes());
        headerAccessor.setUser(session.getPrincipal());
        headerAccessor.setLeaveMutable(true);

        return headerAccessor;
    }

    private WebSocketSession mockWebSocketSession(String sessionId) {
        Session nativeSession = mock(Session.class);
        when(nativeSession.getId()).thenReturn(sessionId);
        when(nativeSession.getUserPrincipal()).thenReturn(mock(Principal.class));

        StandardWebSocketSession wsSession = spy(new StandardWebSocketSession(null,
                                                                          null,
                                                                          null,
                                                                          null));
        
        when(wsSession.getId()).thenReturn(sessionId);
        wsSession.initializeNativeSession(nativeSession);

        return wsSession;
    }

    private ExecutionResult stubExecutionResult(Flux<ExecutionResult> mockStompRelayObservable,
                                                CountDownLatch completeDownLatch) {
        ExecutionResult executionResult = mock(ExecutionResult.class);
        when(executionResult.getErrors()).thenReturn(Collections.emptyList());

        doAnswer(new Answer<Publisher<ExecutionResult>>() {

            @Override
            public Publisher<ExecutionResult> answer(InvocationOnMock invocation) throws Throwable {

                ConnectableFlux<ExecutionResult> connectableObservable = mockStompRelayObservable.share()
                                                                                                       .publish();
                Disposable handle = connectableObservable.connect();

                return connectableObservable.onBackpressureBuffer()
                                            .doOnComplete(() -> {
                                                completeDownLatch.countDown();
                                            })
                                            .doOnCancel(() -> {
                                                handle.dispose();
                                            });

            }
        }).when(executionResult)
          .getData();

        return executionResult;

    }

}
