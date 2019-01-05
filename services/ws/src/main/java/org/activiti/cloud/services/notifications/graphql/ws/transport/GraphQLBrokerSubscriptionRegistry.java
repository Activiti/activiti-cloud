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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.util.Assert;


public class GraphQLBrokerSubscriptionRegistry {

	private final ConcurrentHashMap<String, SessionSubscriptionInfo> subscriptionRegistry;

    public GraphQLBrokerSubscriptionRegistry() {
		this.subscriptionRegistry = new ConcurrentHashMap<>();
	}

    public void subscribe(String sessionId, String subscriptionId, GraphQLBrokerChannelSubscriber subscriber) {
    	this.subscribe(sessionId, subscriptionId, subscriber, null);
    }

    public void subscribe(String sessionId, String subscriptionId, GraphQLBrokerChannelSubscriber subscriber, Runnable callback) {
    	SessionSubscriptionInfo sessionSubscription = Optional.ofNullable(subscriptionRegistry.get(sessionId))
			.orElseGet(() -> {
				SessionSubscriptionInfo subscriptionInfo = new SessionSubscriptionInfo(sessionId);
				subscriptionRegistry.put(sessionId, subscriptionInfo);

				return subscriptionInfo;
			});

    	sessionSubscription.addSubscription(subscriptionId, subscriber);

		if(callback != null) {
			callback.run();
		}
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
    	unsubscribe(sessionId, subscriptionId, null);
    }

    public void unsubscribe(String sessionId, String subscriptionId, Consumer<GraphQLBrokerChannelSubscriber> callback) {
    	SessionSubscriptionInfo subscriptionInfo = subscriptionRegistry.get(sessionId);

    	if(subscriptionInfo != null)  {
    		GraphQLBrokerChannelSubscriber subscriber = subscriptionInfo.removeSubscription(subscriptionId);
    		if(callback != null) {
    			if(subscriber != null)
    				callback.accept(subscriber);
    		}
    	}

    }

    public void unsubscribe(String sessionId,  Consumer<GraphQLBrokerChannelSubscriber> callback) {
    	SessionSubscriptionInfo subscriptionInfo = subscriptionRegistry.remove(sessionId);

    	if(subscriptionInfo != null)  {
    		subscriptionInfo.removeAll().forEach(subscriber -> {
	    		if(callback != null) {
	    			if(subscriber != null)
	    				callback.accept(subscriber);
	    		}
			});
    	}
    }


    public SessionSubscriptionInfo get(String sessionId) {
        return subscriptionRegistry.getOrDefault(sessionId, new SessionSubscriptionInfo(sessionId));
    }


	/**
	 * Hold subscriptions for a session.
	 */
    public static class SessionSubscriptionInfo {

		private final String sessionId;

		// subscriptionId -> subscribers
		private final Map<String, GraphQLBrokerChannelSubscriber> subscriberLookup =
				new ConcurrentHashMap<String, GraphQLBrokerChannelSubscriber>(4);

		public SessionSubscriptionInfo(String sessionId) {
			Assert.notNull(sessionId, "'sessionId' must not be null");
			this.sessionId = sessionId;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		public Set<String> getSubscriptions() {
			return this.subscriberLookup.keySet();
		}

		public GraphQLBrokerChannelSubscriber getSubscriber(String subscriptionId) {
			return this.subscriberLookup.get(subscriptionId);
		}

		public void addSubscription(String subscriptionId, GraphQLBrokerChannelSubscriber subscriber) {
			subscriberLookup.put(subscriptionId, subscriber);
		}

		public GraphQLBrokerChannelSubscriber removeSubscription(String destination) {
			return this.subscriberLookup.remove(destination);
		}

		public Collection<GraphQLBrokerChannelSubscriber> removeAll() {
			Collection<GraphQLBrokerChannelSubscriber> values = new ArrayList<>(this.subscriberLookup.values());
			this.subscriberLookup.clear();

			return values;
		}

		@Override
		public String toString() {
			return "[sessionId=" + this.sessionId + ", subscriptions=" + this.subscriberLookup + "]";
		}
	}

}
