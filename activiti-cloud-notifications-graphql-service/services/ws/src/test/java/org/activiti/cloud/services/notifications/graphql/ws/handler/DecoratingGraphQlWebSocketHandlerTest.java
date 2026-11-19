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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.context.ContextSnapshotFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

class DecoratingGraphQlWebSocketHandlerTest {

    private WebGraphQlHandler webGraphQlHandler;
    private CorsConfiguration corsConfiguration;

    @BeforeEach
    void setUp() {
        webGraphQlHandler = mock(WebGraphQlHandler.class);
        when(webGraphQlHandler.contextSnapshotFactory()).thenReturn(ContextSnapshotFactory.builder().build());
        corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("https://example.org");
    }

    @Test
    void should_applyDecorators_when_decoratorFactoriesAreRegistered() {
        DecoratingGraphQlWebSocketHandler handler = handlerWith(
            List.of(
                delegate -> new NamedDecorator("outer", delegate),
                delegate -> new NamedDecorator("inner", delegate)
            )
        );

        WebSocketHttpRequestHandler requestHandler = handler.initWebSocketHttpRequestHandler(
            new DefaultHandshakeHandler()
        );

        assertThat(activitiChainOf(requestHandler))
            .extracting(DecoratingGraphQlWebSocketHandlerTest::describe)
            .containsExactly("outer", "inner", DecoratingGraphQlWebSocketHandler.class.getSimpleName());
    }

    @Test
    void should_exposeGraphQlHandlerDirectly_when_noDecoratorFactoryIsRegistered() {
        DecoratingGraphQlWebSocketHandler handler = handlerWith(List.of());

        WebSocketHttpRequestHandler requestHandler = handler.initWebSocketHttpRequestHandler(
            new DefaultHandshakeHandler()
        );

        assertThat(activitiChainOf(requestHandler)).containsExactly(handler);
    }

    @Test
    void should_keepSpringHandshakeInterceptors_when_decorated() {
        DecoratingGraphQlWebSocketHandler handler = handlerWith(
            List.of(delegate -> new NamedDecorator("outer", delegate))
        );
        GraphQlWebSocketHandler springHandler = new GraphQlWebSocketHandler(
            webGraphQlHandler,
            new JacksonJsonHttpMessageConverter(),
            Duration.ofSeconds(60),
            Duration.ofSeconds(5),
            corsConfiguration
        );

        WebSocketHttpRequestHandler requestHandler = handler.initWebSocketHttpRequestHandler(
            new DefaultHandshakeHandler()
        );

        assertThat(requestHandler.getHandshakeInterceptors())
            .extracting(Object::getClass)
            .containsExactlyElementsOf(
                springHandler
                    .initWebSocketHttpRequestHandler(new DefaultHandshakeHandler())
                    .getHandshakeInterceptors()
                    .stream()
                    .map(Object::getClass)
                    .toList()
            )
            .contains(OriginHandshakeInterceptor.class);
    }

    @Test
    void should_keepGraphQlSubProtocols_when_decorated() {
        DecoratingGraphQlWebSocketHandler handler = handlerWith(
            List.of(delegate -> new NamedDecorator("outer", delegate))
        );

        WebSocketHttpRequestHandler requestHandler = handler.initWebSocketHttpRequestHandler(
            new DefaultHandshakeHandler()
        );

        assertThat(WebSocketHandlerDecorator.unwrap(requestHandler.getWebSocketHandler())).isSameAs(handler);
        assertThat(handler.getSubProtocols()).contains("graphql-transport-ws");
    }

    private DecoratingGraphQlWebSocketHandler handlerWith(List<GraphQlWebSocketHandlerDecoratorFactory> factories) {
        return new DecoratingGraphQlWebSocketHandler(
            webGraphQlHandler,
            new JacksonJsonHttpMessageConverter(),
            Duration.ofSeconds(60),
            Duration.ofSeconds(5),
            corsConfiguration,
            factories
        );
    }

    private static List<WebSocketHandler> activitiChainOf(WebSocketHttpRequestHandler requestHandler) {
        List<WebSocketHandler> chain = new ArrayList<>();
        WebSocketHandler current = requestHandler.getWebSocketHandler();
        while (current instanceof WebSocketHandlerDecorator decorator) {
            if (!decorator.getClass().getPackageName().startsWith("org.springframework")) {
                chain.add(decorator);
            }
            current = decorator.getDelegate();
        }
        chain.add(current);
        return chain;
    }

    private static String describe(WebSocketHandler handler) {
        return handler instanceof NamedDecorator namedDecorator
            ? namedDecorator.name
            : handler.getClass().getSimpleName();
    }

    private static final class NamedDecorator extends WebSocketHandlerDecorator {

        private final String name;

        private NamedDecorator(String name, WebSocketHandler delegate) {
            super(delegate);
            this.name = name;
        }
    }
}
