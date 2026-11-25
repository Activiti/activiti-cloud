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
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

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
        Assert.notNull(decoratorFactories, "decoratorFactories must not be null");
        this.decoratorFactories = List.copyOf(decoratorFactories);
    }

    @Override
    public WebSocketHttpRequestHandler initWebSocketHttpRequestHandler(HandshakeHandler handshakeHandler) {
        WebSocketHttpRequestHandler endpoint = super.initWebSocketHttpRequestHandler(handshakeHandler);
        if (decoratorFactories.isEmpty()) {
            return endpoint;
        }
        WebSocketHandler handler = this;
        for (GraphQlWebSocketHandlerDecoratorFactory factory : decoratorFactories.reversed()) {
            handler = factory.decorate(handler);
        }
        WebSocketHttpRequestHandler decorated = new WebSocketHttpRequestHandler(handler, handshakeHandler);
        decorated.setHandshakeInterceptors(endpoint.getHandshakeInterceptors());
        return decorated;
    }
}
