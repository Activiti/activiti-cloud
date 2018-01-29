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
package org.activiti.cloud.services.query.qraphql.ws.transport;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;

public class GraphQLBrokerSubProtocolHandler implements SubProtocolHandler, ApplicationEventPublisherAware {

	public static final int MINIMUM_WEBSOCKET_MESSAGE_SIZE = 16 * 1024 + 256;

	private static final Log logger = LogFactory.getLog(GraphQLBrokerSubProtocolHandler.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Map<String, Principal> graphqlAuthentications = new ConcurrentHashMap<String, Principal>();

	private final Stats stats = new Stats();

	private ApplicationEventPublisher eventPublisher;

	@Override
	public List<String> getSupportedProtocols() {
		return Collections.singletonList("graphql-ws");
	}

	@Override
	public void handleMessageFromClient(WebSocketSession session,
										WebSocketMessage<?> message,
										MessageChannel outputChannel) throws Exception
	{
		if(message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;

			GraphQLMessage payload = objectMapper.reader()
					.forType(GraphQLMessage.class)
					.readValue(textMessage.getPayload());

			try {

				SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

				headerAccessor.setDestination("/graphql/ws");
				headerAccessor.setSessionId(session.getId());
				headerAccessor.setSessionAttributes(session.getAttributes());
				headerAccessor.setUser(getUser(session));
				headerAccessor.setLeaveMutable(true);
				Message<GraphQLMessage> decodedMessage = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());

				headerAccessor.setHeader(StompHeaderAccessor.HEART_BEAT_HEADER, new long[] {0, 5000});

//				if (!detectImmutableMessageInterceptor(outputChannel)) {
//					headerAccessor.setImmutable();
//				}

				if (logger.isTraceEnabled()) {
					logger.trace("From client: " + headerAccessor.getShortLogMessage(message.getPayload()));
				}

				boolean isConnect = GraphQLMessageType.CONNECTION_TERMINATE.equals(payload.getType());
				if (isConnect) {
					this.stats.incrementConnectCount();
				}
				else if (GraphQLMessageType.CONNECTION_TERMINATE.equals(payload.getType())) {
					this.stats.incrementDisconnectCount();
				}

				try {
					SimpAttributesContextHolder.setAttributesFromMessage(decodedMessage);
					boolean sent = outputChannel.send(decodedMessage);

					if (sent) {
						if (isConnect) {
							Principal user = headerAccessor.getUser();
							if (user != null && user != session.getPrincipal()) {
								this.graphqlAuthentications.put(session.getId(), user);
							}
						}
						if (this.eventPublisher != null) {
							if (isConnect) {
								publishEvent(new GraphQLSessionConnectEvent(this, decodedMessage, getUser(session)));
							}
							else if (GraphQLMessageType.START.equals(payload.getType())) {
								publishEvent(new GraphQLSessionSubscribeEvent(this, decodedMessage, getUser(session)));
							}
							else if (GraphQLMessageType.STOP.equals(payload.getType())) {
								publishEvent(new GraphQLSessionUnsubscribeEvent(this, decodedMessage, getUser(session)));
							}
						}
					}
				}
				finally {
					SimpAttributesContextHolder.resetAttributes();
				}
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to send client message to application via MessageChannel" +
							" in session " + session.getId() + ". Sending STOMP ERROR to client.", ex);
				}
				sendErrorMessage(session, ex, payload);
			}

		}

		return;
	}

	@Override
	public void handleMessageToClient(WebSocketSession session, Message<?> message) {

		if (!(message.getPayload() instanceof GraphQLMessage)) {
			logger.error("Expected OperationMessage. Ignoring " + message + ".");
			return;
		}

		boolean closeWebSocketSession = false;
		try {
			GraphQLMessage operation = (GraphQLMessage) message.getPayload();

			if(GraphQLMessageType.CONNECTION_ACK.equals(operation.getType()))
				this.stats.incrementConnectedCount();

			byte[] bytes = objectMapper.writer()
					.writeValueAsBytes(message.getPayload());

			session.sendMessage(new TextMessage(bytes));
		}
		catch (SessionLimitExceededException ex) {
			// Bad session, just get out
			throw ex;
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			logger.debug("Failed to send WebSocket message to client in session "
					+ session.getId() + ".", ex);
			closeWebSocketSession = true;
		}
		finally {
			if (closeWebSocketSession) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException ex) {
					// Ignore
				}
			}
		}

	}

	@Override
	public String resolveSessionId(Message<?> message) {
		return SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
	}

	@Override
	public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception {
		if (session.getTextMessageSizeLimit() < MINIMUM_WEBSOCKET_MESSAGE_SIZE) {
			session.setTextMessageSizeLimit(MINIMUM_WEBSOCKET_MESSAGE_SIZE);
		}
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel)
			throws Exception {
		/*
		 * To cleanup we send an internal messages to the handlers. It might be possible
		 * that this is an unexpected session end and the client did not unsubscribe his
		 * subscriptions.
		 */

        Message<GraphQLMessage> message = createDisconnectMessage(session);

		try {
			SimpAttributesContextHolder.setAttributesFromMessage(message);
			if (this.eventPublisher != null) {
				Principal user = getUser(session);
				publishEvent(new GraphQLSessionDisconnectEvent(this, message, session.getId(), closeStatus, user));
			}

			outputChannel.send(message);
		}
		finally {
			SimpAttributesContextHolder.resetAttributes();
		}
	}

	private Message<GraphQLMessage> createDisconnectMessage(WebSocketSession session) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

		headerAccessor.setDestination("/graphql/ws");
		headerAccessor.setSessionId(session.getId());
		headerAccessor.setSessionAttributes(session.getAttributes());
		headerAccessor.setUser(getUser(session));
		headerAccessor.setLeaveMutable(false);

        GraphQLMessage operation = new GraphQLMessage(null, GraphQLMessageType.CONNECTION_TERMINATE, Collections.emptyMap());

        return MessageBuilder.createMessage(operation, headerAccessor.getMessageHeaders());
	}


	/**
	 * Invoked when no
	 * {@link #setErrorHandler(StompSubProtocolErrorHandler) errorHandler}
	 * is configured to send an ERROR frame to the client.
	 */
	protected void sendErrorMessage(WebSocketSession session, Throwable error, GraphQLMessage message) {

		GraphQLMessage response = new GraphQLMessage(message.getId(), GraphQLMessageType.CONNECTION_ERROR, Collections.emptyMap());

		ObjectWriter writer = objectMapper.writer();

		try {
			byte[] bytes = writer.writeValueAsBytes(response);

			session.sendMessage(new TextMessage(bytes));
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			logger.debug("Failed to send ERROR to client", ex);
		}
	}
	private Principal getUser(WebSocketSession session) {
		Principal user = this.graphqlAuthentications.get(session.getId());
		return user != null ? user : session.getPrincipal();
	}

	private static class Stats {

		private final AtomicInteger connect = new AtomicInteger();

		private final AtomicInteger connected = new AtomicInteger();

		private final AtomicInteger disconnect = new AtomicInteger();

		public void incrementConnectCount() {
			this.connect.incrementAndGet();
		}

		public void incrementConnectedCount() {
			this.connected.incrementAndGet();
		}

		public void incrementDisconnectCount() {
			this.disconnect.incrementAndGet();
		}

		@Override
		public String toString() {
			return "processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")";
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	private void publishEvent(ApplicationEvent event) {
		try {
			this.eventPublisher.publishEvent(event);
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Error publishing " + event, ex);
			}
		}
	}



}
