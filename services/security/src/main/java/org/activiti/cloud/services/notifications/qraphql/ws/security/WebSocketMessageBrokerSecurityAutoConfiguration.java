/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@ConditionalOnProperty(name="spring.activiti.cloud.services.notification.graphql.ws.security.enabled", matchIfMissing = true)
public class WebSocketMessageBrokerSecurityAutoConfiguration {

    @Configuration
    @PropertySources(value= {
            @PropertySource(value="classpath:META-INF/graphql-security.properties"),
            @PropertySource(value="classpath:graphql-security.properties", ignoreResourceNotFound = true)
    })
    public static class DefaultWebSocketMessageBrokerSecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebSocketMessageBrokerSecurityConfigurer graphQLSecurityWebSocketMessageBrokerConfiguration() {
            return new WebSocketMessageBrokerSecurityConfigurer();
        }
        
        @Bean
        @ConditionalOnMissingBean
        public JWSAuthenticationInterceptorConfigurer jwsTokenChannelSecurityContextConfigurer(JWSAuthenticationManager keycloakWebSocketAuthManager) {
            return new JWSAuthenticationInterceptorConfigurer(keycloakWebSocketAuthManager);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public KeycloakSecurityContextInerceptorConfigurer jwsTokenChannelAuthenticationConfigurer(KeycloakAccessTokenVerifier keycloakTokenVerifier) {
            return new KeycloakSecurityContextInerceptorConfigurer(keycloakTokenVerifier);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public KeycloakAccessTokenVerifier keycloakTokenVerifier(KeycloakProperties keycloakProperties) {
            return new KeycloakAccessTokenVerifier(keycloakProperties);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public JWSAuthenticationManager keycloakWebSocketAuthManager(KeycloakAccessTokenVerifier keycloakTokenVerifier) {
            return new JWSAuthenticationManager(keycloakTokenVerifier);
        }
    }
}
