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

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;

/**
 * Event raised when the session of a WebSocket client is closed.
 *
 * <p>Note that this event may be raised more than once for a single session and
 * therefore event consumers should be idempotent and ignore a duplicate event.
 *
 */
@SuppressWarnings("serial")
public class GraphQLSessionDisconnectEvent extends AbstractGraphQLSubProtocolEvent {

	private final String sessionId;

	private final CloseStatus status;


	/**
	 * Create a new SessionDisconnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the message
	 * @param sessionId the disconnect message
	 * @param closeStatus the status object
	 */
	public GraphQLSessionDisconnectEvent(Object source, Message<GraphQLMessage> message, String sessionId,
			CloseStatus closeStatus) {

		this(source, message, sessionId, closeStatus, null);
	}

	/**
	 * Create a new SessionDisconnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the message
	 * @param sessionId the disconnect message
	 * @param closeStatus the status object
	 * @param user the current session user
	 */
	public GraphQLSessionDisconnectEvent(Object source, Message<GraphQLMessage> message, String sessionId,
			CloseStatus closeStatus, Principal user) {

		super(source, message, user);
		Assert.notNull(sessionId, "Session id must not be null");
		this.sessionId = sessionId;
		this.status = closeStatus;
	}


	/**
	 * Return the session id.
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Return the status with which the session was closed.
	 */
	public CloseStatus getCloseStatus() {
		return this.status;
	}


	@Override
	public String toString() {
		return "SessionDisconnectEvent[sessionId=" + this.sessionId + ", " +
				(this.status != null ? this.status.toString() : "closeStatus=null") + "]";
	}

}
