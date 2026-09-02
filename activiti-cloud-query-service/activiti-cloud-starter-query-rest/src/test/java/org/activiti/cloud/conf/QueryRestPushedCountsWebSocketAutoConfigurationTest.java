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
package org.activiti.cloud.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.notifications.graphql.ws.config.GraphQLWebSocketMessageBrokerAutoConfiguration;
import org.activiti.cloud.services.notifications.qraphql.ws.security.WebSocketMessageBrokerSecurityAutoConfiguration;
import org.activiti.cloud.services.query.rest.subscriber.PushedCountsWebSocketInterceptor;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * The Step 3 "spike": boots a minimal servlet web application context with the real
 * autoconfiguration chain, including {@link WebSocketMessageBrokerSecurityAutoConfiguration} -
 * the same autoconfiguration the engine-events websocket uses - to empirically confirm that the
 * placeholder {@code graphql/pushed-counts.graphqls} schema is sufficient to activate the
 * websocket transport, and that {@link PushedCountsWebSocketInterceptor} wins as the single
 * {@link WebSocketGraphQlInterceptor} while that autoconfiguration's own {@code
 * authenticationInterceptor} correctly backs off (spring-graphql throws at handler-construction
 * time if there is more than one).
 */
class QueryRestPushedCountsWebSocketAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration.class,
                WebMvcAutoConfiguration.class,
                DispatcherServletAutoConfiguration.class,
                GraphQlAutoConfiguration.class,
                GraphQlWebMvcAutoConfiguration.class,
                GraphQLWebSocketMessageBrokerAutoConfiguration.class,
                WebSocketMessageBrokerSecurityAutoConfiguration.class,
                QueryRestPushedCountsWebSocketAutoConfiguration.class
            )
        )
        .withPropertyValues("activiti.cloud.services.oauth2.iam-name=test")
        .withUserConfiguration(StubSecurityBeans.class)
        // WebApplicationContextRunner does not install Boot's Duration-aware conversion
        // service by default (unlike a real SpringApplication run), which the
        // @Value(...) Duration parameters on the pushed-counts beans need.
        .withBean("conversionService", ConversionService.class, ApplicationConversionService::new);

    @Test
    void should_activateTheWebsocketTransport_and_wireExactlyOnePushedCountsInterceptor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebGraphQlHandler.class);
            assertThat(context).hasSingleBean(PushedCountsWebSocketInterceptor.class);
            assertThat(context.getBeansOfType(WebSocketGraphQlInterceptor.class)).hasSize(1);
            assertThat(context).hasSingleBean(SubscriberRegistry.class);

            // Confirms the handler actually resolved this bean as ITS single websocket
            // interceptor, not merely that both happen to coexist unused in the context.
            WebGraphQlHandler handler = context.getBean(WebGraphQlHandler.class);
            assertThat(handler.getWebSocketInterceptor()).isInstanceOf(PushedCountsWebSocketInterceptor.class);
        });
    }

    @Configuration
    static class StubSecurityBeans {

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        JwtAccessTokenValidator jwtAccessTokenValidator() {
            return new JwtAccessTokenValidator(List.of());
        }

        @Bean
        JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter() {
            return mock(JwtUserInfoUriAuthenticationConverter.class);
        }
    }
}
