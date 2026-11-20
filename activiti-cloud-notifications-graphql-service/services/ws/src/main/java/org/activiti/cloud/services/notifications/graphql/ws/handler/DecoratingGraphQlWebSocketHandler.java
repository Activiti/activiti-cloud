/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.ws.handler;

import java.time.Duration;
import java.util.List;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

/**
 * Spring's {@link GraphQlWebSocketHandler}, exposed to clients through the {@link GraphQlWebSocketHandlerDecoratorFactory}
 * decorators registered in the application. The GraphQL over WebSocket protocol handling itself is unchanged.
 */
public class DecoratingGraphQlWebSocketHandler extends GraphQlWebSocketHandler {

    private final List<GraphQlWebSocketHandlerDecoratorFactory> decoratorFactories;

    public DecoratingGraphQlWebSocketHandler(
        WebGraphQlHandler graphQlHandler,
        HttpMessageConverter<Object> converter,
        Duration connectionInitTimeout,
        Duration keepAliveDuration,
        CorsConfiguration corsConfiguration,
        List<GraphQlWebSocketHandlerDecoratorFactory> decoratorFactories
    ) {
        super(graphQlHandler, converter, connectionInitTimeout, keepAliveDuration, corsConfiguration);
        this.decoratorFactories = List.copyOf(decoratorFactories);
    }

    @Override
    public WebSocketHttpRequestHandler initWebSocketHttpRequestHandler(HandshakeHandler handshakeHandler) {
        WebSocketHttpRequestHandler undecorated = super.initWebSocketHttpRequestHandler(handshakeHandler);
        WebSocketHttpRequestHandler decorated = new WebSocketHttpRequestHandler(decorate(this), handshakeHandler);
        decorated.setHandshakeInterceptors(undecorated.getHandshakeInterceptors());
        return decorated;
    }

    private WebSocketHandler decorate(WebSocketHandler handler) {
        WebSocketHandler decorated = handler;
        for (GraphQlWebSocketHandlerDecoratorFactory factory : decoratorFactories.reversed()) {
            decorated = factory.decorate(decorated);
        }
        return decorated;
    }
}
