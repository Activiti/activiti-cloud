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
package org.activiti.cloud.services.notifications.graphql.ws.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.notifications.graphql.ws.handler.DecoratingGraphQlWebSocketHandler;
import org.activiti.cloud.services.notifications.graphql.ws.handler.GraphQlWebSocketHandlerDecoratorFactory;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessMonitor;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessProperties;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.LivenessGraphQlWebSocketHandler;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

class GraphQlWebSocketHandlerAutoConfigurationTest {

    private static final String WEBSOCKET_PATH = "/v2/ws/graphql";
    private static final String LIVENESS = GraphQlWebSocketLivenessProperties.PREFIX;

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
        AutoConfigurations.of(
            GraphQlAutoConfiguration.class,
            GraphQlWebMvcAutoConfiguration.class,
            GraphQLWebSocketMessageBrokerAutoConfiguration.class,
            GraphQlWebSocketHandlerAutoConfiguration.class
        )
    );

    @Test
    void should_decorateWebSocketEndpointWithLivenessCheck_when_defaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DecoratingGraphQlWebSocketHandler.class);
            assertThat(context).hasSingleBean(GraphQlWebSocketLivenessMonitor.class);
            assertThat(context.getBean(GraphQlWebSocketLivenessMonitor.class).getTimeout()).isEqualTo(
                Duration.ofSeconds(60)
            );
            assertThat(endpointChain(context)).hasExactlyElementsOfTypes(
                LivenessGraphQlWebSocketHandler.class,
                DecoratingGraphQlWebSocketHandler.class
            );
            assertThat(endpointChain(context).getLast()).isSameAs(context.getBean(GraphQlWebSocketHandler.class));
        });
    }

    @Test
    void should_runLivenessMonitorUntilContextIsClosed_when_defaultConfiguration() {
        contextRunner.run(context -> {
            GraphQlWebSocketLivenessMonitor monitor = context.getBean(GraphQlWebSocketLivenessMonitor.class);
            assertThat(monitor.isRunning()).isTrue();

            context.close();

            assertThat(monitor.isRunning()).isFalse();
        });
    }

    @Test
    void should_useConfiguredLivenessTimeout_when_timeoutIsSet() {
        contextRunner
            .withPropertyValues(LIVENESS + ".timeout=PT30S")
            .run(context ->
                assertThat(context.getBean(GraphQlWebSocketLivenessMonitor.class).getTimeout()).isEqualTo(
                    Duration.ofSeconds(30)
                )
            );
    }

    @Test
    void should_notDecorateWebSocketEndpointWithLivenessCheck_when_livenessIsDisabled() {
        contextRunner.withPropertyValues(LIVENESS + ".enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(GraphQlWebSocketLivenessMonitor.class);
            assertThat(endpointChain(context)).hasExactlyElementsOfTypes(DecoratingGraphQlWebSocketHandler.class);
        });
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "timeout=PT0S        | timeout must be positive",
            "timeout=-PT10S      | timeout must be positive",
            "check-interval=PT0S | check-interval must be positive",
            "check-interval=-PT1S| check-interval must be positive",
            "check-interval=PT60S| must be shorter than",
            "check-interval=PT61S| must be shorter than",
        }
    )
    void should_failStartup_when_livenessDurationsAreInvalid(String property, String expectedMessage) {
        contextRunner.withPropertyValues(LIVENESS + "." + property).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .rootCause()
                .hasMessageContaining(LIVENESS)
                .hasMessageContaining(expectedMessage);
        });
    }

    @Test
    void should_failStartup_when_keepAliveIsNotConfigured() {
        contextRunner.withPropertyValues("spring.graphql.websocket.keep-alive=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .rootCause()
                .hasMessageContaining("requires spring.graphql.websocket.keep-alive");
        });
    }

    @Test
    void should_startWithoutLivenessCheck_when_keepAliveIsNotConfiguredAndLivenessIsDisabled() {
        contextRunner
            .withPropertyValues("spring.graphql.websocket.keep-alive=", LIVENESS + ".enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(GraphQlWebSocketLivenessMonitor.class);
            });
    }

    @Test
    void should_notConfigureWebSocketDecoration_when_webSocketPathIsNotConfigured() {
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    GraphQlAutoConfiguration.class,
                    GraphQlWebMvcAutoConfiguration.class,
                    GraphQlWebSocketHandlerAutoConfiguration.class
                )
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(GraphQlWebSocketHandler.class);
                assertThat(context).doesNotHaveBean(GraphQlWebSocketLivenessMonitor.class);
            });
    }

    @Test
    void should_keepCorsInterceptorAndBootHandler_when_endpointIsDecorated() {
        contextRunner.withPropertyValues("spring.graphql.cors.allowed-origins=https://example.org").run(context -> {
            assertThat(endpoint(context).getHandshakeInterceptors())
                .filteredOn(OriginHandshakeInterceptor.class::isInstance)
                .singleElement()
                .extracting(interceptor -> ((OriginHandshakeInterceptor) interceptor).getAllowedOrigins())
                .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                .containsExactly("https://example.org");
            assertThat(endpointChain(context).getLast()).isSameAs(context.getBean(GraphQlWebSocketHandler.class));
        });
    }

    @Test
    void should_backOffWithLivenessCheck_when_applicationDefinesItsOwnHandlerWithoutKeepAliveProperty() {
        contextRunner
            .withUserConfiguration(CustomWebSocketHandlerConfiguration.class)
            .withPropertyValues("spring.graphql.websocket.keep-alive=")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(GraphQlWebSocketHandler.class);
                assertThat(context).doesNotHaveBean(DecoratingGraphQlWebSocketHandler.class);
                assertThat(context).doesNotHaveBean(GraphQlWebSocketLivenessMonitor.class);
                assertThat(endpointChain(context)).containsExactly(
                    context.getBean("customGraphQlWebSocketHandler", WebSocketHandler.class)
                );
            });
    }

    @Test
    void should_applyDecoratorsByOrderValue_when_orderedFactoriesAreRegisteredInReverseOrder() {
        contextRunner
            .withUserConfiguration(OrderedFactoriesConfiguration.class)
            .run(context ->
                assertThat(endpointChain(context))
                    .extracting(GraphQlWebSocketHandlerAutoConfigurationTest::describe)
                    .containsExactly(
                        "LivenessGraphQlWebSocketHandler",
                        "outer",
                        "inner",
                        "DecoratingGraphQlWebSocketHandler"
                    )
            );
    }

    @Test
    void should_applyDecoratorsByBeanMethodOrder_when_annotatedFactoriesAreRegisteredInReverseOrder() {
        contextRunner
            .withUserConfiguration(AnnotatedFactoriesConfiguration.class)
            .run(context ->
                assertThat(endpointChain(context))
                    .extracting(GraphQlWebSocketHandlerAutoConfigurationTest::describe)
                    .containsExactly(
                        "LivenessGraphQlWebSocketHandler",
                        "outer",
                        "inner",
                        "DecoratingGraphQlWebSocketHandler"
                    )
            );
    }

    @Test
    void should_notDecorateWebSocketHandler_when_graphQlWebSocketIsDisabled() {
        contextRunner
            .withPropertyValues("spring.activiti.cloud.services.query.graphql.ws.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(DecoratingGraphQlWebSocketHandler.class);
                assertThat(context).doesNotHaveBean(GraphQlWebSocketLivenessMonitor.class);
            });
    }

    private static String describe(WebSocketHandler handler) {
        return handler instanceof NamedDecorator namedDecorator
            ? namedDecorator.name
            : handler.getClass().getSimpleName();
    }

    private static WebSocketHttpRequestHandler endpoint(AssertableWebApplicationContext context) {
        WebSocketHandlerMapping mapping = context.getBean("graphQlWebSocketMapping", WebSocketHandlerMapping.class);
        return (WebSocketHttpRequestHandler) mapping.getUrlMap().get(WEBSOCKET_PATH);
    }

    private static List<WebSocketHandler> endpointChain(AssertableWebApplicationContext context) {
        List<WebSocketHandler> chain = new ArrayList<>();
        WebSocketHandler current = endpoint(context).getWebSocketHandler();
        while (current instanceof WebSocketHandlerDecorator decorator) {
            if (!decorator.getClass().getPackageName().startsWith("org.springframework")) {
                chain.add(decorator);
            }
            current = decorator.getDelegate();
        }
        chain.add(current);
        return chain;
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomWebSocketHandlerConfiguration {

        @Bean
        GraphQlWebSocketHandler customGraphQlWebSocketHandler(WebGraphQlHandler webGraphQlHandler) {
            return new GraphQlWebSocketHandler(
                webGraphQlHandler,
                new JacksonJsonHttpMessageConverter(),
                Duration.ofSeconds(60),
                Duration.ofSeconds(5)
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedFactoriesConfiguration {

        @Bean
        GraphQlWebSocketHandlerDecoratorFactory innerDecoratorFactory() {
            return new OrderedFactory("inner", 2);
        }

        @Bean
        GraphQlWebSocketHandlerDecoratorFactory outerDecoratorFactory() {
            return new OrderedFactory("outer", 1);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedFactoriesConfiguration {

        @Bean
        @Order(2)
        GraphQlWebSocketHandlerDecoratorFactory innerDecoratorFactory() {
            return handler -> new NamedDecorator("inner", handler);
        }

        @Bean
        @Order(1)
        GraphQlWebSocketHandlerDecoratorFactory outerDecoratorFactory() {
            return handler -> new NamedDecorator("outer", handler);
        }
    }

    private record OrderedFactory(String name, int order) implements GraphQlWebSocketHandlerDecoratorFactory, Ordered {
        @Override
        public WebSocketHandlerDecorator decorate(WebSocketHandler handler) {
            return new NamedDecorator(name, handler);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    private static final class NamedDecorator extends WebSocketHandlerDecorator {

        private final String name;

        private NamedDecorator(String name, WebSocketHandler delegate) {
            super(delegate);
            this.name = name;
        }
    }
}
