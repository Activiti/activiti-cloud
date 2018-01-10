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
package org.activiti.cloud.services.query.qraphql.ws.datafetcher;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Cancellable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class StompRelayObservableEmitterHandler extends StompSessionHandlerAdapter implements Cancellable {

    private static Logger log = LoggerFactory.getLogger(StompRelayObservableEmitterHandler.class);

	private final List<String> destinations;
	private final ObservableEmitter<Map<String, Object>> emitter;

	private StompSession session;

	/**
	 * @param destinations
	 * @param emitter
	 */
	public StompRelayObservableEmitterHandler(List<String> destinations, ObservableEmitter<Map<String, Object>> emitter) {
		this.destinations = destinations;
		this.emitter = emitter;

		emitter.setCancellable(this);
	}

	@Override
	public Type getPayloadType(StompHeaders headers) {
		return Map.class;
	}

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		this.session = session;

		log.info("Stomp session connected: {}", session.getSessionId());

		for (String destination : destinations) {
			Subscription subs = session.subscribe("/topic/" + destination, this);
			log.info("Subscribed {}['{}'] with Stomp session: {}", subs.getSubscriptionId(), destination, session.getSessionId());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		emitter.onNext((Map) payload);
	}

	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
			Throwable exception) {
		log.error(exception.getMessage(), exception);
		emitter.onError(exception);
	}

	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
		log.error(exception.getMessage(), exception);
		emitter.onError(exception);
	}

	@Override
	public void cancel() throws Exception {
		if(session != null) {
			log.info("Stomp session canceled: {}", session.getSessionId());
			session.disconnect();
		}
	}

}
