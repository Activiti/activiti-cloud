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

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

public class StompRelayPublisherFactory {

    private static Logger log = LoggerFactory.getLogger(StompRelayPublisherFactory.class);

    private StompRelayDestinationResolver destinationResolver =
            new SimpleStompRelayDataFetcherDestinationResolver();

    private String login = "guest";
    private String passcode = "guest";

    private final ReactorNettyTcpStompClient stompClient;

    public StompRelayPublisherFactory(ReactorNettyTcpStompClient stompClient) {
        this.stompClient = stompClient;
    }

    public Publisher<Map<String,Object>> getPublisher(DataFetchingEnvironment environment) {
        Observable<Map<String,Object>> stompRelayObservable = Observable.create(emitter -> {

            List<String> destinations = destinationResolver.resolveDestinations(environment);

            StompSessionHandler handler = new StompRelayObservableEmitterHandler(destinations, emitter);

            StompHeaders stompHeaders = new StompHeaders();
            stompHeaders.setLogin(login);
            stompHeaders.setPasscode(passcode);

            stompClient.connect(stompHeaders, handler);
        });

        ConnectableObservable<Map<String, Object>> connectableObservable =
                stompRelayObservable
                    .share()
                    .publish();

        Disposable handle = connectableObservable.connect();

        return connectableObservable
                .toFlowable(BackpressureStrategy.BUFFER)
                .doOnCancel(() -> {
                    handle.dispose();
                });
    }


    public StompRelayPublisherFactory login(String login) {
        this.login = login;

        return this;
    }


    public StompRelayPublisherFactory passcode(String passcode) {
        this.passcode = passcode;

        return this;
    }

    /**
     * @param destinationResolver
     */
    public StompRelayPublisherFactory destinationResolver(StompRelayDestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;

        return this;
    }

}
