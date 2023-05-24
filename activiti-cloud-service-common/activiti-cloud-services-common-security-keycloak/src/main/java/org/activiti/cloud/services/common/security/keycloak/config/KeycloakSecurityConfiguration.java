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

import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.Scopes;
import java.util.function.Function;
import org.activiti.cloud.common.swagger.springdoc.conf.SwaggerAutoConfiguration;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAdapter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakJwtAdapter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakResourceJwtAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.jwt.Jwt;

@AutoConfiguration
@PropertySource("classpath:keycloak-configuration.properties")
@AutoConfigureBefore(SwaggerAutoConfiguration.class)
@ConditionalOnProperty(
    value = "activiti.cloud.services.oauth2.iam-name",
    havingValue = "keycloak",
    matchIfMissing = true
)
public class KeycloakSecurityConfiguration {

    @Bean
    @ConditionalOnProperty(name = "keycloak.use-resource-role-mappings", havingValue = "false", matchIfMissing = true)
    public Function<Jwt, JwtAdapter> jwtGlobalAdapter() {
        return jwt -> new KeycloakJwtAdapter(jwt);
    }

    @Bean
    @ConditionalOnProperty(name = "keycloak.use-resource-role-mappings", havingValue = "true")
    public Function<Jwt, JwtAdapter> jwtResourceResourceAdapter(@Value("${keycloak.resource}") String resource) {
        return jwt -> new KeycloakResourceJwtAdapter(resource, jwt);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenProvider jwtAccessTokenProvider(Function<Jwt, JwtAdapter> jwtAdapterSupplier) {
        return new JwtAccessTokenProvider(jwtAdapterSupplier);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuthFlow swaggerOAuthFlow(
        @Value("${keycloak.auth-server-url}") String authServer,
        @Value("${keycloak.realm}") String realm
    ) {
        return new OAuthFlow()
            .authorizationUrl(authServer + "/realms/" + realm + "/protocol/openid-connect/auth")
            .scopes(new Scopes());
    }
}
