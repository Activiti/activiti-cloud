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
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

class DecoratingGraphQlWebSocketHandlerTest {

    @Test
    void should_exposeHandlerDirectly_when_noDecoratorFactoryIsGiven() {
        DecoratingGraphQlWebSocketHandler handler = handler(List.of());

        WebSocketHttpRequestHandler endpoint = handler.initWebSocketHttpRequestHandler(new DefaultHandshakeHandler());

        assertThat(activitiChainOf(endpoint)).containsExactly(handler);
    }

    @Test
    void should_applyDecoratorsInListOrder_when_decoratorFactoriesAreGiven() {
        DecoratingGraphQlWebSocketHandler handler = handler(
            List.of(
                delegate -> new NamedDecorator("outer", delegate),
                delegate -> new NamedDecorator("inner", delegate)
            )
        );

        WebSocketHttpRequestHandler endpoint = handler.initWebSocketHttpRequestHandler(new DefaultHandshakeHandler());

        assertThat(activitiChainOf(endpoint))
            .extracting(DecoratingGraphQlWebSocketHandlerTest::describe)
            .containsExactly("outer", "inner", DecoratingGraphQlWebSocketHandler.class.getSimpleName());
        assertThat(WebSocketHandlerDecorator.unwrap(endpoint.getWebSocketHandler())).isSameAs(handler);
    }

    @Test
    void should_keepSpringDecoratorsAndHandshakeInterceptors_when_decorated() {
        WebSocketHttpRequestHandler undecorated = handler(List.of()).initWebSocketHttpRequestHandler(
            new DefaultHandshakeHandler()
        );

        WebSocketHttpRequestHandler decorated = handler(
            List.of(delegate -> new NamedDecorator("outer", delegate))
        ).initWebSocketHttpRequestHandler(new DefaultHandshakeHandler());

        assertThat(chainOf(decorated)).hasSize(chainOf(undecorated).size() + 1);
        assertThat(decorated.getHandshakeInterceptors())
            .extracting(Object::getClass)
            .containsExactlyElementsOf(undecorated.getHandshakeInterceptors().stream().map(Object::getClass).toList())
            .contains(OriginHandshakeInterceptor.class);
    }

    private static DecoratingGraphQlWebSocketHandler handler(List<GraphQlWebSocketHandlerDecoratorFactory> factories) {
        WebGraphQlHandler webGraphQlHandler = mock(WebGraphQlHandler.class);
        when(webGraphQlHandler.contextSnapshotFactory()).thenReturn(ContextSnapshotFactory.builder().build());
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("https://example.org");
        return new DecoratingGraphQlWebSocketHandler(
            webGraphQlHandler,
            new JacksonJsonHttpMessageConverter(),
            Duration.ofSeconds(60),
            Duration.ofSeconds(5),
            corsConfiguration,
            factories
        );
    }

    private static List<WebSocketHandler> chainOf(WebSocketHttpRequestHandler endpoint) {
        List<WebSocketHandler> chain = new ArrayList<>();
        WebSocketHandler current = endpoint.getWebSocketHandler();
        while (current instanceof WebSocketHandlerDecorator decorator) {
            chain.add(decorator);
            current = decorator.getDelegate();
        }
        chain.add(current);
        return chain;
    }

    private static List<WebSocketHandler> activitiChainOf(WebSocketHttpRequestHandler endpoint) {
        return chainOf(endpoint)
            .stream()
            .filter(handler -> !handler.getClass().getPackageName().startsWith("org.springframework.web.socket"))
            .toList();
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
