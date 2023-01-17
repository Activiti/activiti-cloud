/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import java.util.function.Function;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.jwt.JwtAdapter;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakJwtAdapter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakResourceJwtAdapter;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessTokenVerifier;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.jwt.JwtAccessTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
@ConditionalOnProperty(name="spring.activiti.cloud.services.notification.graphql.ws.security.enabled", matchIfMissing = true)
@Import(WebSocketMessageBrokerSecurityConfigurer.class)
public class WebSocketMessageBrokerSecurityAutoConfiguration {

    @Configuration
    @PropertySources(value= {
            @PropertySource(value="classpath:META-INF/graphql-security.properties"),
            @PropertySource(value="classpath:graphql-security.properties", ignoreResourceNotFound = true)
    })
    public static class DefaultWebSocketMessageBrokerSecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public JWSAuthenticationInterceptorConfigurer jwsTokenChannelSecurityContextConfigurer(JWSAuthenticationManager keycloakWebSocketAuthManager) {
            return new JWSAuthenticationInterceptorConfigurer(keycloakWebSocketAuthManager);
        }

        @Bean
        @ConditionalOnMissingBean
        public JwtInterceptorConfigurer jwsTokenChannelAuthenticationConfigurer(GraphQLAccessTokenVerifier keycloakTokenVerifier) {
            return new JwtInterceptorConfigurer(keycloakTokenVerifier);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnExpression("'${activiti.cloud.services.oauth2.iam-name}'!='keycloak'")
        public GraphQLAccessTokenVerifier jwtTokenVerifier(JwtAccessTokenValidator jwtAccessTokenValidator,
                                                           JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter,
                                                           JwtDecoder jwtDecoder) {
            return new JwtAccessTokenVerifier(jwtAccessTokenValidator, jwtUserInfoUriAuthenticationConverter, jwtDecoder, jwt -> jwt.getClaimAsStringList("role"));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "activiti.cloud.services.oauth2.iam-name", havingValue = "keycloak")
        public GraphQLAccessTokenVerifier keycloakTokenVerifier(JwtAccessTokenValidator jwtAccessTokenValidator,
                                                                JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter,
                                                                JwtDecoder jwtDecoder,
                                                                Function<Jwt, JwtAdapter> jwtAdapterSupplier) {
            return new JwtAccessTokenVerifier(jwtAccessTokenValidator, jwtUserInfoUriAuthenticationConverter, jwtDecoder, jwt -> jwtAdapterSupplier.apply(jwt).getRoles());
        }


        @Bean
        @ConditionalOnMissingBean
        public JWSAuthenticationManager keycloakWebSocketAuthManager(GraphQLAccessTokenVerifier keycloakTokenVerifier) {
            return new JWSAuthenticationManager(keycloakTokenVerifier);
        }
    }
}
