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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import graphql.ExecutionResult;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

public class GraphQLBrokerChannelSubscriber implements Subscriber<ExecutionResult>{

    private static Logger log = LoggerFactory.getLogger(GraphQLBrokerChannelSubscriber.class);

	private final MessageChannel outboundChannel;

	private final Message<?> message;

	private final String operationMessageId;

	private final UnicastProcessor<ExecutionResult> processor = UnicastProcessor.create();

    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    
    private final Disposable control;

	public GraphQLBrokerChannelSubscriber(Message<?> message,  String operationMessageId,
			MessageChannel outboundChannel,
			long bufferTimeSpanMs, int bufferCount)
	{
		this.outboundChannel = outboundChannel;
		this.operationMessageId = operationMessageId;
		this.message = message;

        this.control = Flux.from(processor)
                           .map(ExecutionResult::getData)
                           .subscribe(this::sendDataToClient);
	}

	public void cancel() {
	    control.dispose();
	    
        Subscription subscription = subscriptionRef.get();

        log.info("Cancel subscription {}", subscription);
        
        if (subscription != null) {
            try {
                subscription.cancel();
            } catch(Exception ignore) { }
        }
	}

    @Override
    public void onSubscribe(Subscription s) {
        log.info("New subscription: {}", s);
        subscriptionRef.set(s);
        
        requestNext(1);
    }

    @Override
    public void onNext(ExecutionResult executionResult) {
        log.debug("Process {} executionResult {} ", subscriptionRef.get(), executionResult);
    	processor.onNext(executionResult);

        requestNext(1);
    }

    @Override
    public void onError(Throwable t) {
        log.error("Subscription {} threw an exception {}", subscriptionRef.get(), t);

        Map<String, Object> payload = Collections.singletonMap("errors", Collections.singletonList(t.getMessage()));

        GraphQLMessage operationMessage = new GraphQLMessage(operationMessageId, GraphQLMessageType.ERROR, payload);

		Message<GraphQLMessage> responseMessage =
				MessageBuilder.createMessage(operationMessage, getMessageHeaders());

		outboundChannel.send(responseMessage);
    }

    @Override
    public void onComplete() {
        log.info("Subscription complete: {}", subscriptionRef.get());

        cancel();

        GraphQLMessage operationMessage = new GraphQLMessage(operationMessageId, GraphQLMessageType.COMPLETE);

		Message<?> responseMessage = MessageBuilder.createMessage(operationMessage, getMessageHeaders());

		outboundChannel.send(responseMessage);
    }

    private void requestNext(int n) {
        Subscription subscription = subscriptionRef.get();
        if (subscription != null) {
            subscription.request(n);
        }
    }

    protected void sendDataToClient(Object data) {
	    Map<String, Object> payload = Collections.singletonMap("data", data);
	    GraphQLMessage operationData = new GraphQLMessage(operationMessageId, GraphQLMessageType.DATA, payload);

		Message<?> responseMessage = MessageBuilder.createMessage(operationData, getMessageHeaders());

		// Send message directly to user
	    outboundChannel.send(responseMessage);
    }
    
    private MessageHeaders getMessageHeaders() {
        MessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.getMutableAccessor(message);
        headerAccessor.setLeaveMutable(true); // must be mutable to preserve publish order!
        
        return headerAccessor.getMessageHeaders();
    }

}
