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
package org.activiti.cloud.services.common.security.keycloak.config;

import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakJwtAdapter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakResourceJwtAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:keycloak-configuration.properties")
@ConditionalOnProperty(
    value = "activiti.cloud.services.oauth2.iam-name",
    havingValue = "keycloak",
    matchIfMissing = true
)
public class KeycloakSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "keycloak.use-resource-role-mappings", havingValue = "false", matchIfMissing = true)
    public JwtAccessTokenProvider jwtGlobalAccessTokenProvider() {
        return new JwtAccessTokenProvider(jwt -> new KeycloakJwtAdapter(jwt));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "keycloak.use-resource-role-mappings", havingValue = "true")
    public JwtAccessTokenProvider jwtResourceAccessTokenProvider(@Value("${keycloak.resource}") String resource) {
        return new JwtAccessTokenProvider(jwt -> new KeycloakResourceJwtAdapter(resource, jwt));
    }
}
