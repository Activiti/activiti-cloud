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
import org.springframework.messaging.Message;

/**
 * Event raised when a new WebSocket client using a GraphQL Messaging Protocol
 * as the WebSocket sub-protocol issues a connect request.
 *
 * <p>Note that this is not the same as the WebSocket session getting established
 * but rather the client's first attempt to connect within the sub-protocol,
 * for example sending the CONNECT frame.
 *
 */
@SuppressWarnings("serial")
public class GraphQLSessionUnsubscribeEvent extends AbstractGraphQLSubProtocolEvent {

	/**
	 * Create a new SessionConnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the connect message
	 */
	public GraphQLSessionUnsubscribeEvent(Object source, Message<GraphQLMessage> message) {
		super(source, message);
	}

	public GraphQLSessionUnsubscribeEvent(Object source, Message<GraphQLMessage> message, Principal user) {
		super(source, message, user);
	}

}
