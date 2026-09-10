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
import org.activiti.cloud.common.feature.FeatureToggleAutoConfiguration;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalGroupsProviderChain;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.notifications.graphql.ws.config.GraphQLWebSocketMessageBrokerAutoConfiguration;
import org.activiti.cloud.services.notifications.qraphql.ws.security.WebSocketMessageBrokerSecurityAutoConfiguration;
import org.activiti.cloud.services.query.app.CountConsumer;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import reactor.core.publisher.Sinks;

/**
 * Confirms {@code countConsumer} - the missing half of the pushed-counts broker relay - is wired
 * exactly when {@code activiti.cloud.query.pushed-counts.enabled=true}, and not otherwise, since it
 * depends on the {@code pushedCountsSink} bean that
 * {@link QueryRestPushedCountsWebSocketAutoConfiguration} only creates under that same property.
 * The actual broker round trip (destination -&gt; function binding -&gt; this bean) is covered by
 * {@link org.activiti.cloud.services.query.app.CountConsumerTest} at the unit level; assembling the
 * full Spring Cloud Stream function-binding machinery in a narrow test context here would test the
 * framework, not this code.
 */
class PushedCountsMessagingBridgeAutoConfigurationTest {

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
                QueryRestPushedCountsWebSocketAutoConfiguration.class,
                FeatureToggleAutoConfiguration.class,
                PushedCountsMessagingBridgeAutoConfiguration.class
            )
        )
        .withPropertyValues("activiti.cloud.services.oauth2.iam-name=test")
        .withUserConfiguration(StubSecurityBeans.class)
        // WebApplicationContextRunner doesn't install Boot's Duration-aware conversion service by
        // default, which the @Value Duration params on the pushed-counts beans need.
        .withBean("conversionService", ConversionService.class, ApplicationConversionService::new);

    @Test
    void should_wireTheCountConsumerBean_when_thePropertyIsExplicitlyEnabled() {
        contextRunner
            .withPropertyValues("activiti.cloud.query.pushed-counts.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(Sinks.Many.class);
                assertThat(context.getBean("countConsumer")).isInstanceOf(CountConsumer.class);
            });
    }

    @Test
    void should_notWireTheCountConsumerBean_when_thePropertyIsNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(Sinks.Many.class)).isEmpty();
            assertThat(context.containsBean("countConsumer")).isFalse();
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

        @Bean
        JwtPrincipalGroupsProviderChain principalGroupsProvider() {
            return mock(JwtPrincipalGroupsProviderChain.class);
        }
    }
}
