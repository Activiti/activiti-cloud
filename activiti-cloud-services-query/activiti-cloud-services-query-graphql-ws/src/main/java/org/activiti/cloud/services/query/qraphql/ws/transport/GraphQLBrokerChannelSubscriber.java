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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import graphql.ExecutionResult;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

public class GraphQLBrokerChannelSubscriber implements Subscriber<ExecutionResult>{

    private static Logger log = LoggerFactory.getLogger(GraphQLBrokerChannelSubscriber.class);

	private final MessageChannel outboundChannel;

	private final MessageHeaderAccessor headerAccessor;

	private final String operationMessageId;

	private final PublishSubject<ExecutionResult> publishSubject = PublishSubject.create();

    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

	public GraphQLBrokerChannelSubscriber(Message<?> message,  String operationMessageId,
			MessageChannel outboundChannel,
			long bufferTimeSpanMs, int bufferCount)
	{
		this.outboundChannel = outboundChannel;
		this.operationMessageId = operationMessageId;
		this.headerAccessor = SimpMessageHeaderAccessor.getMutableAccessor(message);

		publishSubject
			.map(ExecutionResult::getData)
			.buffer(bufferTimeSpanMs, TimeUnit.MILLISECONDS, bufferCount)
			.subscribe(this::sendDataToClient);
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
    public void onNext(ExecutionResult executionResult) {
    	publishSubject.onNext(executionResult);

        requestNext(1);
    }

    @Override
    public void onError(Throwable t) {
        log.error("Subscription threw an exception", t);

        Map<String, Object> payload = Collections.singletonMap("errors", Collections.singletonList(t.getMessage()));

        GraphQLMessage operationMessage = new GraphQLMessage(operationMessageId, GraphQLMessageType.ERROR, payload);

		Message<GraphQLMessage> responseMessage =
				MessageBuilder.createMessage(operationMessage, headerAccessor.getMessageHeaders());

		outboundChannel.send(responseMessage);
    }

    @Override
    public void onComplete() {
        log.info("Subscription complete");

        cancel();

        GraphQLMessage operationMessage = new GraphQLMessage(operationMessageId, GraphQLMessageType.COMPLETE, Collections.emptyMap());

		Message<?> responseMessage = MessageBuilder.createMessage(operationMessage, headerAccessor.getMessageHeaders());

		outboundChannel.send(responseMessage);
    }

    private void requestNext(int n) {
        Subscription subscription = subscriptionRef.get();
        if (subscription != null) {
            subscription.request(n);
        }
    }

    protected void sendDataToClient(List<Object> data) {
    	if(data.size() > 0) {
		    Map<String, Object> payload = Collections.singletonMap("data", data);
		    GraphQLMessage operationData = new GraphQLMessage(operationMessageId, GraphQLMessageType.DATA, payload);

			Message<?> responseMessage = MessageBuilder.createMessage(operationData, headerAccessor.getMessageHeaders());

			// Send message directly to user
		    outboundChannel.send(responseMessage);
    	}
    }

}
