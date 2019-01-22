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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class GraphQLMessage {

	private Map<String, Object> payload;
	private String id;
	private GraphQLMessageType type;

	GraphQLMessage() {
	}
	
    public GraphQLMessage(String id, GraphQLMessageType type) {
        this(id, type, Collections.emptyMap());
    }
	
	public GraphQLMessage(String id, GraphQLMessageType type, Map<String, Object> payload) {
		super();
		this.payload = payload;
		this.id = id;
		this.type = type;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public String getId() {
		return id;
	}

	public GraphQLMessageType getType() {
		return type;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
        if (!(other instanceof GraphQLMessage)) {
			return false;
		}
		GraphQLMessage otherMsg = (GraphQLMessage) other;
		return (Objects.equals(this.payload, otherMsg.getPayload()));
	}

	@Override
	public int hashCode() {
		return (Objects.hashCode(this.payload) * 23);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" [payload=");
		sb.append(this.payload);
        sb.append(", id=").append(this.id);
        sb.append(", type=").append(this.type).append("]");
		return sb.toString();
	}

}
