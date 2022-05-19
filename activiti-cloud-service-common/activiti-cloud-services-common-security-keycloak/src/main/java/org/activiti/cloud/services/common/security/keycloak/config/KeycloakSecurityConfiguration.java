package org.activiti.cloud.services.common.security.keycloak.config;

import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakJwtAdapter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakResourceJwtAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
    public JwtAccessTokenProvider jwtResourceAccessTokenProvider(@Value("${keycloak.resource}" ) String resource) {
        return new JwtAccessTokenProvider(jwt -> new KeycloakResourceJwtAdapter(resource, jwt));
    }

}
