/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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
import java.util.concurrent.atomic.AtomicReference;

import graphql.ExecutionResult;
import org.activiti.cloud.services.query.qraphql.ws.util.JsonConverter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class GraphQLSubscriptionResultWebSocketSubscriber implements Subscriber<ExecutionResult>{

    private static Logger log = LoggerFactory.getLogger(GraphQLSubscriptionResultWebSocketSubscriber.class);

	private final WebSocketSession session;

    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

	public GraphQLSubscriptionResultWebSocketSubscriber(WebSocketSession session) {
		this.session = session;
	}

	public void cancel() {
        Subscription subscription = subscriptionRef.get();
        if (subscription != null) {
            subscription.cancel();
        }
	}

    @Override
    public void onSubscribe(Subscription s) {
        subscriptionRef.set(s);
        requestNext(1);
    }

    @Override
    public void onNext(ExecutionResult er) {
        log.debug("Sending onNext");
        try {
            Object data = er.getData();
            TextMessage message = new TextMessage(JsonConverter.toJsonString(data));
            session.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestNext(1);
    }

    @Override
    public void onError(Throwable t) {
        log.error("Subscription threw an exception", t);
        try {
        	session.close(CloseStatus.SERVER_ERROR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public void onComplete() {
        log.info("Subscription complete");
        try {
        	session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    private void requestNext(int n) {
        Subscription subscription = subscriptionRef.get();
        if (subscription != null) {
            subscription.request(n);
        }
    }

}
