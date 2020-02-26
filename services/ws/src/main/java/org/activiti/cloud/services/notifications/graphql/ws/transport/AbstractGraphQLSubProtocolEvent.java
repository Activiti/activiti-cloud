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

import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A base class for events for a message received from a WebSocket client and
 * parsed into a higher-level GraphQL WS sub-protocol.
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractGraphQLSubProtocolEvent extends ApplicationEvent {

	private final Message<GraphQLMessage> message;

	private final Principal user;


	/**
	 * Create a new AbstractGraphQLSubProtocolEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the incoming message
	 */
	protected AbstractGraphQLSubProtocolEvent(Object source, Message<GraphQLMessage> message) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = null;
	}

	/**
	 * Create a new AbstractGraphQLSubProtocolEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the incoming message
	 */
	protected AbstractGraphQLSubProtocolEvent(Object source, Message<GraphQLMessage> message, Principal user) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = user;
	}


	/**
	 * Return the Message associated with the event. Here is an example of
	 * obtaining information about the session id or any headers in the
	 * message:
	 * <pre class="code">
	 * StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
	 * headers.getSessionId();
	 * headers.getSessionAttributes();
	 * headers.getPrincipal();
	 * </pre>
	 */
	public Message<GraphQLMessage> getMessage() {
		return this.message;
	}

	/**
	 * Return the user for the session associated with the event.
	 */
	public Principal getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.message + "]";
	}

}