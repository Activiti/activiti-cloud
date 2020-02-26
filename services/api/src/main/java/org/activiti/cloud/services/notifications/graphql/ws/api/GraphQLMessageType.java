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
package org.activiti.cloud.services.notifications.graphql.ws.api;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GraphQLMessageType {

	CONNECTION_INIT("connection_init"), // Client -> Server
	CONNECTION_ACK("connection_ack"), // Server -> Client
	CONNECTION_ERROR("connection_error"), // Server -> Client
	// NOTE: The keep alive message type does not follow the standard due to
	// connection optimizations
	KA("ka"), // Server -> Client
	CONNECTION_TERMINATE("connection_terminate"), // Client -> Server
	START("start"), // Client -> Server
	DATA("data"), // Server -> Client
	ERROR("error"), // Server -> Client
	COMPLETE("complete"), // Server -> Client
	STOP("stop"); // Client -> Server

	private final String type;

	GraphQLMessageType(String type) {
		this.type = type;
	}

	// ****** Reverse Lookup Implementation************//

	// Lookup table
	private static final Map<String, GraphQLMessageType> lookup = new HashMap<>();

	// Populate the lookup table on loading time
	static {
		for (GraphQLMessageType env : GraphQLMessageType.values()) {
			lookup.put(env.type, env);
		}
	}

	@JsonCreator
	// This method can be used for reverse lookup purpose
	public static GraphQLMessageType get(String type) {
		return lookup.get(type);
	}

	@JsonValue
	@Override
	public String toString() {
		return type;
	}
}
