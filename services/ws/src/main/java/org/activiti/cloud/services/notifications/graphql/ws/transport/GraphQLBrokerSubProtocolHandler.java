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

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.annotation.Gauge;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
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
import org.springframework.web.socket.messaging.SubProtocolHandler;

public class GraphQLBrokerSubProtocolHandler implements SubProtocolHandler, ApplicationEventPublisherAware {

    private static final int DEFAULT_KA_INTERVAL = 5000;

    private static final String KA_INTERVAL_HEADER = "kaInterval";

    private static final String X_AUTHORIZATION = "X-Authorization";

    private static final String GRAPHQL_MESSAGE_TYPE = "graphQLMessageType";

    public static final String GRAPHQL_WS = "graphql-ws";

    public static final int MINIMUM_WEBSOCKET_MESSAGE_SIZE = 16 * 1024 + 256;

	private static final Logger logger = LoggerFactory.getLogger(GraphQLBrokerSubProtocolHandler.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Map<String, Principal> graphqlAuthentications = new ConcurrentHashMap<String, Principal>();

	private final Stats stats = new Stats();

	private ApplicationEventPublisher eventPublisher;

	private final String destination;

    private ScheduledFuture<?> loggingTask;

    private ScheduledExecutorService taskScheduler = Executors.newSingleThreadScheduledExecutor();

    private long loggingPeriod = 5 * 60 * 1000;

    public GraphQLBrokerSubProtocolHandler(String destination) {
        this.destination = destination;
        setLoggingPeriod(loggingPeriod);
    }

	@Override
	public List<String> getSupportedProtocols() {
		return Collections.singletonList(GRAPHQL_WS);
	}

	@Override
	public void handleMessageFromClient(WebSocketSession session,
										WebSocketMessage<?> message,
										MessageChannel outputChannel) throws Exception
	{
		if(message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;

			GraphQLMessage sourceMessage = objectMapper.reader()
					.forType(GraphQLMessage.class)
					.readValue(textMessage.getPayload());

			try {
				SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

				headerAccessor.setDestination(destination);
				headerAccessor.setSessionId(session.getId());
				headerAccessor.setSessionAttributes(session.getAttributes());
				headerAccessor.setUser(getUser(session));
				headerAccessor.setLeaveMutable(true);
				Message<GraphQLMessage> decodedMessage = MessageBuilder.createMessage(sourceMessage, headerAccessor.getMessageHeaders());

                headerAccessor.setHeader(GRAPHQL_MESSAGE_TYPE, sourceMessage.getType().toString());

				if (logger.isTraceEnabled()) {
					logger.trace("From client: " + headerAccessor.getShortLogMessage(message.getPayload()));
				}

				boolean isConnect = GraphQLMessageType.CONNECTION_INIT.equals(sourceMessage.getType());
				if (isConnect) {
					this.stats.incrementConnectCount();
					
                    // Let's inject connectionParams into headers
					Optional.ofNullable(sourceMessage.getPayload())
					    .ifPresent(map -> {
					        map.entrySet().forEach(e-> {
        					   headerAccessor.setHeader(e.getKey(), e.getValue());               
					        });
					    });

	                // inject client KA interval
	                Integer kaInterval = Optional.ofNullable(headerAccessor.getHeader(KA_INTERVAL_HEADER))
	                                             .map(v -> Integer.parseInt(v.toString()))
	                                             .orElse(DEFAULT_KA_INTERVAL);
    	                
	                headerAccessor.setHeader(StompHeaderAccessor.HEART_BEAT_HEADER, new long[] {0, kaInterval});
				}
				else if (GraphQLMessageType.CONNECTION_TERMINATE.equals(sourceMessage.getType())) {
					this.stats.incrementDisconnectCount();
				}
				else if (GraphQLMessageType.START.equals(sourceMessage.getType())) {
                    this.stats.incrementStartCount();
                }
                else if (GraphQLMessageType.STOP.equals(sourceMessage.getType())) {
                    this.stats.incrementStopCount();
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
							else if (GraphQLMessageType.START.equals(sourceMessage.getType())) {
								publishEvent(new GraphQLSessionSubscribeEvent(this, decodedMessage, getUser(session)));
							}
							else if (GraphQLMessageType.STOP.equals(sourceMessage.getType())) {
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
							" in session " + session.getId() + ". Sending CONNECTION_ERROR to client.", ex);
				}
				sendErrorMessage(session, ex, sourceMessage);
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

        this.stats.incrementDisconnectCount();

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
            this.graphqlAuthentications.remove(session.getId());

			SimpAttributesContextHolder.resetAttributes();
		}
	}

	private Message<GraphQLMessage> createDisconnectMessage(WebSocketSession session) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

		headerAccessor.setDestination(destination);
		headerAccessor.setSessionId(session.getId());
		headerAccessor.setSessionAttributes(session.getAttributes());
		headerAccessor.setUser(getUser(session));
		headerAccessor.setLeaveMutable(false);

        GraphQLMessage operation = new GraphQLMessage(null, GraphQLMessageType.CONNECTION_TERMINATE);

        return MessageBuilder.createMessage(operation, headerAccessor.getMessageHeaders());
	}


	/**
     * Invoked to send an ERROR frame to the client.
     */
	protected void sendErrorMessage(WebSocketSession session, Throwable error, GraphQLMessage message) {
        this.stats.incrementErrorCount();

		GraphQLMessage response = new GraphQLMessage(message.getId(), GraphQLMessageType.CONNECTION_ERROR);

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

        private final AtomicInteger start = new AtomicInteger();

        private final AtomicInteger stop = new AtomicInteger();

        private final AtomicInteger error = new AtomicInteger();

    	@Gauge
        public Integer connectCount() {
        	return this.connect.get();
        }

    	@Gauge
        public Integer connectedCount() {
        	return this.connected.get();
        }

    	@Gauge
        public Integer disconnectCount() {
        	return this.disconnect.get();
        }

    	@Gauge
        public Integer startCount() {
        	return this.start.get();
        }

    	@Gauge
        public Integer stopCount() {
        	return this.stop.get();
        }

    	@Gauge
        public Integer errorCount() {
        	return this.error.get();
        }
    	
        
		public void incrementConnectCount() {
			this.connect.incrementAndGet();
		}

		public void incrementConnectedCount() {
			this.connected.incrementAndGet();
		}

		public void incrementDisconnectCount() {
			this.disconnect.incrementAndGet();
		}

        public void incrementStartCount() {
            this.start.incrementAndGet();
        }

        public void incrementStopCount() {
            this.stop.incrementAndGet();
        }

        public void incrementErrorCount() {
            this.error.incrementAndGet();
        }

		@Override
		public String toString() {
			return "processed CONNECT(" + this.connect.get() + ")"
			        + "-CONNECTED(" +	this.connected.get() + ")"
                    + "-START(" +   this.start.get() + ")"
                    + "-STOP(" +   this.stop.get() + ")"
                    + "-ERROR(" +   this.error.get() + ")"
			        + "-DISCONNECT(" + this.disconnect.get() + ")";
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

    @Nullable
    private ScheduledFuture<?> initLoggingTask(long initialDelay) {
        if (this.taskScheduler != null && this.loggingPeriod > 0 && logger.isInfoEnabled()) {
            return this.taskScheduler.scheduleAtFixedRate(() ->
                            logger.info(GRAPHQL_WS+"["+ this.stats.toString()+"]"),
                    initialDelay, this.loggingPeriod, TimeUnit.MILLISECONDS);
        }
        return null;
    }

    /**
     * Set the frequency for logging information at INFO level in milliseconds.
     * If set 0 or less than 0, the logging task is cancelled.
     * <p>By default this property is set to 5 minutes (5 * 60 * 1000).
     */
    public void setLoggingPeriod(long period) {
        if (this.loggingTask != null) {
            this.loggingTask.cancel(true);
        }
        this.loggingPeriod = period;
        this.loggingTask = initLoggingTask(0);
    }

    /**
     * Return the configured logging period frequency in milliseconds.
     */
    public long getLoggingPeriod() {
        return this.loggingPeriod;
    }



}
