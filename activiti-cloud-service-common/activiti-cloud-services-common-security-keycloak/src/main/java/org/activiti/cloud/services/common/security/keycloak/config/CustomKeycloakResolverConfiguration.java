package org.activiti.cloud.services.common.security.keycloak.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomKeycloakResolverConfiguration {

    @Bean("keycloakConfigResolver")
    public KeycloakConfigResolver KeycloakConfigResolver(AdapterConfig adapterConfig) {
        return new CustomKeycloakSpringConfigResolver(adapterConfig);
    }
}
