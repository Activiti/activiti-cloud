package org.activiti.cloud.services.identity.keycloak.config;

import org.activiti.cloud.services.identity.keycloak.ActivitiKeycloakProperties;
import org.activiti.cloud.services.identity.keycloak.KeycloakAuthorizationLookup;
import org.activiti.cloud.services.identity.keycloak.KeycloakIdentityLookup;
import org.activiti.cloud.services.identity.keycloak.KeycloakInstanceWrapper;
import org.activiti.cloud.services.identity.keycloak.KeycloakLookupService;
import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.keycloak.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ActivitiKeycloakProperties.class,KeycloakProperties.class})
public class ActivitiKeycloakAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(KeycloakInstanceWrapper.class)
    public KeycloakInstanceWrapper keycloakInstanceWrapper(){
        return new KeycloakInstanceWrapper();
    }

    @Bean
    @ConditionalOnMissingBean(KeycloakLookupService.class)
    public KeycloakLookupService keycloakLookupService(){
        return new KeycloakLookupService();
    }

    @Bean
    @ConditionalOnMissingBean(KeycloakIdentityLookup.class)
    public KeycloakIdentityLookup keycloakIdentityLookup(KeycloakLookupService keycloakLookupService){
        return new KeycloakIdentityLookup(keycloakLookupService);
    }

    @Bean
    @ConditionalOnMissingBean(KeycloakAuthorizationLookup.class)
    public KeycloakAuthorizationLookup keycloakAuthorizationLookup(KeycloakLookupService keycloakLookupService){
        return new KeycloakAuthorizationLookup(keycloakLookupService);
    }
}
