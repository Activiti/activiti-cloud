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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.codahale.metrics.annotation.Timed;
import graphql.ExecutionResult;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.activiti.cloud.services.notifications.graphql.ws.util.QueryParameters;
import org.reactivestreams.Publisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

public class GraphQLBrokerMessageHandler extends AbstractBrokerMessageHandler {

    public final static String BROKER_NOT_AVAILABLE = "Broker Not Available.";

	private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<String, SessionInfo>();

	private MessageHeaderInitializer headerInitializer;

	private TaskScheduler taskScheduler;

	private long[] heartbeatValue;

    private AtomicBoolean brokerAvailable = new AtomicBoolean(false);

	private ScheduledFuture<?> heartbeatFuture;

    private final GraphQLSubscriptionExecutor graphQLSubscriptionExecutor;

    private final GraphQLBrokerSubscriptionRegistry graphQLsubscriptionRegistry;

	private long bufferTimeSpanMs = 1000;

	private int bufferCount = 50;

	public GraphQLBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, GraphQLSubscriptionExecutor graphQLSubscriptionExecutor) {
		super(inboundChannel, outboundChannel, brokerChannel);

		this.graphQLSubscriptionExecutor = graphQLSubscriptionExecutor;
		this.graphQLsubscriptionRegistry = new GraphQLBrokerSubscriptionRegistry();
		
		setPreservePublishOrder(true);
	}

    public GraphQLBrokerSubscriptionRegistry getGraphQLsubscriptionRegistry() {
        return graphQLsubscriptionRegistry;
    }

    public long getBufferTimeSpanMs() {
        return bufferTimeSpanMs;
    }

    public GraphQLBrokerMessageHandler setBufferTimeSpanMs(long bufferTimeSpanMs) {
        this.bufferTimeSpanMs = bufferTimeSpanMs;

        return this;
    }

    public int getBufferCount() {
        return bufferCount;
    }

    public GraphQLBrokerMessageHandler setBufferCount(int bufferCount) {
        this.bufferCount = bufferCount;

        return this;
    }

    @EventListener
	public void on(BrokerAvailabilityEvent event) {
	    this.brokerAvailable.set(event.isBrokerAvailable());
	}

    /**
     * Whether the message broker is currently available and able to process messages.
     * <p>Note that this is in addition to the {@link #isRunning()} flag, which
     * indicates whether this message handler is running. In other words the message
     * handler must first be running and then the {@code #isBrokerAvailable()} flag
     * may still independently alternate between being on and off depending on the
     * concrete sub-class implementation.
     * <p>Application components may implement
     * {@code org.springframework.context.ApplicationListener&lt;BrokerAvailabilityEvent&gt;}
     * to receive notifications when broker becomes available and unavailable.
     */
    @Override
    public boolean isBrokerAvailable() {
        return this.brokerAvailable.get();
    }

	@Override
	protected void startInternal() {
		if (getTaskScheduler() != null) {
			long interval = initHeartbeatTaskDelay();
			if (interval > 0) {
				this.heartbeatFuture = this.taskScheduler.scheduleWithFixedDelay(new HeartbeatTask(), interval);
			}
		}
		else {
			Assert.isTrue(getHeartbeatValue() == null ||
					(getHeartbeatValue()[0] == 0 && getHeartbeatValue()[1] == 0),
					"Heartbeat values configured but no TaskScheduler provided");
		}
		publishBrokerAvailableEvent();
	}

	@Override
	protected void stopInternal() {
		publishBrokerUnavailableEvent();
		try {
			if (this.heartbeatFuture != null) {
				this.heartbeatFuture.cancel(true);
			}

		}
		catch (Throwable ex) {
			logger.error("Error in shutdown of TCP client", ex);
		}
	}

	private long initHeartbeatTaskDelay() {
		if (getHeartbeatValue() == null) {
			return 0;
		}
		else if (getHeartbeatValue()[0] > 0 && getHeartbeatValue()[1] > 0) {
			return Math.min(getHeartbeatValue()[0], getHeartbeatValue()[1]);
		}
		else {
			return (getHeartbeatValue()[0] > 0 ? getHeartbeatValue()[0] : getHeartbeatValue()[1]);
		}
	}

    @SuppressWarnings("unchecked")
    @Override
    protected void handleMessageInternal(Message<?> message) {

        MessageHeaders headers = message.getHeaders();
        SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
        String destination = SimpMessageHeaderAccessor.getDestination(headers);
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        Principal user = SimpMessageHeaderAccessor.getUser(headers);

        updateSessionReadTime(sessionId);

        if (!checkDestinationPrefix(destination)) {
            return;
        }

        if (SimpMessageType.MESSAGE.equals(messageType) && message.getPayload() instanceof GraphQLMessage) {
            logMessage(message);
            Message<GraphQLMessage> graphQLMessage = (Message<GraphQLMessage>) message;
            GraphQLMessageType graphQLMessagePayloadType = graphQLMessage.getPayload().getType();

            switch (graphQLMessagePayloadType) {
                case CONNECTION_INIT:

                    if (!isBrokerAvailable()) {
                        sendErrorMessageToClient(BROKER_NOT_AVAILABLE, GraphQLMessageType.CONNECTION_ERROR, message);
                        return;
                    }

                    long[] clientHeartbeat = SimpMessageHeaderAccessor.getHeartbeat(headers);
                    long[] serverHeartbeat = getHeartbeatValue();
                    this.sessions.put(sessionId, new SessionInfo(sessionId, user, clientHeartbeat, serverHeartbeat));

                    handleConnectionInitMessage(graphQLMessage);
                    break;

                case START:
                    // start subscription
                    if (!isBrokerAvailable()) {
                        sendErrorMessageToClient(BROKER_NOT_AVAILABLE, GraphQLMessageType.ERROR, message);
                        return;
                    }

                    handleStartSubscription(graphQLMessage);
                    break;

                case STOP:
                    // stop subscription
                    handleStopSubscription(graphQLMessage);
                    break;

                case CONNECTION_TERMINATE:
                    // end connection
                    handleConnectionTerminate(graphQLMessage);
                    break;

                default:
                    break;
            }

        }
    }

    @Timed
    protected final void handleConnectionInitMessage(Message<GraphQLMessage> message) {
        GraphQLMessage operationPayload = message.getPayload();

        GraphQLMessage connection_ack = new GraphQLMessage(operationPayload.getId(),
                                                           GraphQLMessageType.CONNECTION_ACK);

        MessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.getMutableAccessor(message);

        Message<?> responseMessage = MessageBuilder.createMessage(connection_ack, headerAccessor.getMessageHeaders());

        getClientOutboundChannel().send(responseMessage);

    }

    @Timed
    protected final void handleStartSubscription(Message<GraphQLMessage> message) {
        logger.info("handleStartSubscription for message " + message);
        
        MessageHeaders headers = message.getHeaders();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        GraphQLMessage operationPayload = message.getPayload();
        QueryParameters parameters = null;

        try {
            parameters = QueryParameters.from(operationPayload.getPayload());
        } catch (Exception e) {
            sendErrorMessageToClient(e.getMessage(), GraphQLMessageType.ERROR, message);
            return;
        }
        ExecutionResult executionResult = graphQLSubscriptionExecutor.execute(parameters.getQuery(),
                                                                              parameters.getVariables());
        
        if (executionResult.getErrors().isEmpty()) {
            if (executionResult.getData() == null) {
                sendErrorMessageToClient("Server error!", GraphQLMessageType.ERROR, message);
            }
            else if (executionResult.getData() instanceof Publisher) {
                Optional.of(executionResult.<Publisher<ExecutionResult>> getData())
                        .ifPresent(data -> {
                            MessageChannel outboundChannel = getClientOutboundChannelForSession(sessionId);
                            
                            GraphQLBrokerChannelSubscriber subscriber = new GraphQLBrokerChannelSubscriber(message,
                                                                                                           operationPayload.getId(),
                                                                                                           outboundChannel,
                                                                                                           bufferTimeSpanMs,
                                                                                                           bufferCount);
                            graphQLsubscriptionRegistry.subscribe(sessionId,
                                                                  operationPayload.getId(),
                                                                  subscriber,
                                                                  () -> {
                                                                      data.subscribe(subscriber);
                                                                  });
                        });
            } else {
                handleQueryOrMutation(operationPayload.getId(), executionResult, message);
            }
            
        } else {
            Map<String, Object> payload = Collections.singletonMap("errors", executionResult.getErrors());

            GraphQLMessage startSubscriptionMessage = message.getPayload();
            GraphQLMessage startSubscriptionErrors = new GraphQLMessage(startSubscriptionMessage.getId(),
                                                                        GraphQLMessageType.ERROR,
                                                                        payload);

            Message<GraphQLMessage> errorMessage = MessageBuilder.createMessage(startSubscriptionErrors, headers);

            getClientOutboundChannel().send(errorMessage);
        }

    }
    
    private void handleQueryOrMutation(String id, ExecutionResult result, Message<GraphQLMessage> message) {
            Map<String, Object> payload = Collections.singletonMap("data", result.getData());
            MessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.getMutableAccessor(message);
            
            GraphQLMessage operationData = new GraphQLMessage(id, GraphQLMessageType.DATA, payload);

            Message<?> dataMessage = MessageBuilder.createMessage(operationData, headerAccessor.getMessageHeaders());

            // Send data
            getClientOutboundChannel().send(dataMessage);
            
            GraphQLMessage completeData = new GraphQLMessage(id, GraphQLMessageType.COMPLETE);

            Message<?> completeMessage = MessageBuilder.createMessage(completeData, headerAccessor.getMessageHeaders());
            
            // Send complete
            getClientOutboundChannel().send(completeMessage);
    }    

    @Timed
    protected final void handleStopSubscription(Message<GraphQLMessage> message) {
        logger.info("handleStopSubscription for message " + message);
        
        MessageHeaders headers = message.getHeaders();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        GraphQLMessage operationPayload = message.getPayload();

        graphQLsubscriptionRegistry.unsubscribe(sessionId, operationPayload.getId(), (subscriber) -> {
            subscriber.onComplete();
        });
    }

    @Timed
    protected final void handleConnectionTerminate(Message<GraphQLMessage> message) {
        logger.info("handleConnectionTerminate for message " + message);

        MessageHeaders headers = message.getHeaders();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);

        graphQLsubscriptionRegistry.unsubscribe(sessionId, (subscriber) -> {
            subscriber.cancel();
        });
    }

    private void sendErrorMessageToClient(String errorText, GraphQLMessageType type, Message<?> inputMessage) {
        Map<String, Object> payload = Collections.singletonMap("errors", Collections.singletonList(errorText));
        GraphQLMessage inputOperation = (GraphQLMessage) inputMessage.getPayload();
        GraphQLMessage connectionError = new GraphQLMessage(inputOperation.getId(), type, payload);
        MessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.getMutableAccessor(inputMessage);
        Message<GraphQLMessage> errorMessage = MessageBuilder.createMessage(connectionError,
                                                                            headerAccessor.getMessageHeaders());

        getClientOutboundChannel().send(errorMessage);
    }

    private void updateSessionReadTime(String sessionId) {
        if (sessionId != null) {
            SessionInfo info = this.sessions.get(sessionId);
            if (info != null) {
                info.setLastReadTime(System.currentTimeMillis());
            }
        }
    }

	private void logMessage(Message<?> message) {
		if (logger.isDebugEnabled()) {
			SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			accessor = (accessor != null ? accessor : SimpMessageHeaderAccessor.wrap(message));
			logger.debug("Processing " + accessor.getShortLogMessage(message.getPayload()));
		}
	}

	/**
	 * Configure the {@link org.springframework.scheduling.TaskScheduler} to
	 * use for providing heartbeat support. Setting this property also sets the
	 * {@link #setHeartbeatValue heartbeatValue} to "10000, 10000".
	 * <p>By default this is not set.
	 */
    public GraphQLBrokerMessageHandler setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
		if (this.heartbeatValue == null) {
			this.heartbeatValue = new long[] {5000, 5000};
		}

        return this;
	}

	/**
	 * Return the configured TaskScheduler.
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * Configure the value for the heart-beat settings. The first number
	 * represents how often the server will write or send a heartbeat.
	 * The second is how often the client should write. 0 means no heartbeats.
	 * <p>By default this is set to "0, 0" unless the {@link #setTaskScheduler
	 * taskScheduler} in which case the default becomes "10000,10000"
	 * (in milliseconds).
	 */
    public GraphQLBrokerMessageHandler setHeartbeatValue(long[] heartbeat) {
		if (heartbeat == null || heartbeat.length != 2 || heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
		}
		this.heartbeatValue = heartbeat;

        return this;
	}

	/**
	 * The configured value for the heart-beat settings.
	 */
	public long[] getHeartbeatValue() {
		return this.heartbeatValue;
	}

	/**
	 * Configure a {@link MessageHeaderInitializer} to apply to the headers
	 * of all messages sent to the client outbound channel.
	 * <p>By default this property is not set.
	 */
	public void setHeaderInitializer(MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured header initializer.
	 */
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	private void initHeaders(SimpMessageHeaderAccessor accessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(accessor);
		}
	}

	private void handleDisconnect(String sessionId, Principal user, Message<?> origMessage) {
		this.sessions.remove(sessionId);
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setSessionId(sessionId);
		accessor.setUser(user);

		if (origMessage != null) {
			accessor.setHeader(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER, origMessage);
		}
		initHeaders(accessor);

		GraphQLMessage disconnect = new GraphQLMessage(null, GraphQLMessageType.CONNECTION_ERROR);

		Message<GraphQLMessage> message = MessageBuilder.createMessage(disconnect, accessor.getMessageHeaders());

		getClientOutboundChannel().send(message);
	}

	protected static class SessionInfo {

		/* receiver SHOULD take into account an error margin */
		private static final long HEARTBEAT_MULTIPLIER = 3;

		private final String sessiondId;

		private final Principal user;

		private final long readInterval;

		private final long writeInterval;

		private volatile long lastReadTime;

		private volatile long lastWriteTime;

		public SessionInfo(String sessiondId, Principal user, long[] clientHeartbeat, long[] serverHeartbeat) {
			this.sessiondId = sessiondId;
			this.user = user;
			if (clientHeartbeat != null && serverHeartbeat != null) {
				this.readInterval = (clientHeartbeat[0] > 0 && serverHeartbeat[1] > 0 ?
						Math.max(clientHeartbeat[0], serverHeartbeat[1]) * HEARTBEAT_MULTIPLIER : 0);
				this.writeInterval = (clientHeartbeat[1] > 0 && serverHeartbeat[0] > 0 ?
						Math.max(clientHeartbeat[1], serverHeartbeat[0]) : 0);
			}
			else {
				this.readInterval = 0;
				this.writeInterval = 0;
			}
			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();
		}

		public String getSessiondId() {
			return this.sessiondId;
		}

		public Principal getUser() {
			return this.user;
		}

		public long getReadInterval() {
			return this.readInterval;
		}

		public long getWriteInterval() {
			return this.writeInterval;
		}

		public long getLastReadTime() {
			return this.lastReadTime;
		}

		public void setLastReadTime(long lastReadTime) {
			this.lastReadTime = lastReadTime;
		}

		public long getLastWriteTime() {
			return this.lastWriteTime;
		}

		public void setLastWriteTime(long lastWriteTime) {
			this.lastWriteTime = lastWriteTime;
		}
	}


	private class HeartbeatTask implements Runnable {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			for (SessionInfo info : sessions.values()) {
				if (info.getReadInterval() > 0 && (now - info.getLastReadTime()) > info.getReadInterval()) {
					handleDisconnect(info.getSessiondId(), info.getUser(), null);
				}
				if (info.getWriteInterval() > 0 && (now - info.getLastWriteTime()) > info.getWriteInterval()) {
					SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
					accessor.setSessionId(info.getSessiondId());
					accessor.setUser(info.getUser());
					initHeaders(accessor);
					MessageHeaders headers = accessor.getMessageHeaders();

					GraphQLMessage heartbeat = new GraphQLMessage(null, GraphQLMessageType.KA);

					getClientOutboundChannel().send(MessageBuilder.createMessage(heartbeat, headers));
				}
			}
		}
	}

}
