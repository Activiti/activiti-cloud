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
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessMonitor;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.GraphQlWebSocketLivenessProperties;
import org.activiti.cloud.services.notifications.graphql.ws.liveness.LivenessGraphQlWebSocketHandler;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
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
    void should_replaceBootWebSocketHandler_with_decoratingHandler() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GraphQlWebSocketHandler.class);
            assertThat(context.getBean(GraphQlWebSocketHandler.class)).isInstanceOf(
                DecoratingGraphQlWebSocketHandler.class
            );
        });
    }

    @Test
    void should_decorateWebSocketEndpointWithLivenessCheck_by_default() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GraphQlWebSocketLivenessMonitor.class);
            assertThat(context.getBean(GraphQlWebSocketLivenessMonitor.class).getTimeout()).isEqualTo(
                Duration.ofSeconds(10)
            );
            assertThat(endpointChain(context)).hasExactlyElementsOfTypes(
                LivenessGraphQlWebSocketHandler.class,
                DecoratingGraphQlWebSocketHandler.class
            );
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

    @Test
    void should_failStartup_when_livenessTimeoutIsNotLongerThanKeepAlive() {
        contextRunner.withPropertyValues(LIVENESS + ".timeout=PT5S").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .rootCause()
                .hasMessageContaining("must be longer than spring.graphql.websocket.keep-alive");
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
    void should_keepBootWebSocketSettings_when_handlerIsReplaced() {
        contextRunner
            .withPropertyValues(
                "spring.graphql.websocket.connection-init-timeout=PT20S",
                "spring.graphql.cors.allowed-origins=https://example.org"
            )
            .run(context -> {
                GraphQlWebSocketHandler handler = context.getBean(GraphQlWebSocketHandler.class);

                assertThat(ReflectionTestUtils.getField(handler, "keepAliveDuration")).isEqualTo(Duration.ofSeconds(5));
                assertThat(ReflectionTestUtils.getField(handler, "initTimeoutDuration")).isEqualTo(
                    Duration.ofSeconds(20)
                );
                assertThat(endpoint(context).getHandshakeInterceptors())
                    .filteredOn(OriginHandshakeInterceptor.class::isInstance)
                    .singleElement()
                    .extracting(interceptor -> ((OriginHandshakeInterceptor) interceptor).getAllowedOrigins())
                    .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
                    .containsExactly("https://example.org");
            });
    }

    @Test
    void should_backOff_when_applicationDefinesItsOwnWebSocketHandler() {
        contextRunner.withUserConfiguration(CustomWebSocketHandlerConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(GraphQlWebSocketHandler.class);
            assertThat(context.getBean(GraphQlWebSocketHandler.class)).isNotInstanceOf(
                DecoratingGraphQlWebSocketHandler.class
            );
        });
    }

    @Test
    void should_notReplaceWebSocketHandler_when_graphQlWebSocketIsDisabled() {
        contextRunner
            .withPropertyValues("spring.activiti.cloud.services.query.graphql.ws.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(DecoratingGraphQlWebSocketHandler.class);
                assertThat(context).doesNotHaveBean(GraphQlWebSocketLivenessMonitor.class);
            });
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
}
