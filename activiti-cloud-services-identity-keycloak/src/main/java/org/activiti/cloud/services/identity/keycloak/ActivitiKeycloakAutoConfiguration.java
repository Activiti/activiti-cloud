package org.activiti.cloud.services.identity.keycloak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.keycloak.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ActivitiKeycloakProperties.class,KeycloakProperties.class})
@Import(SecurityConfig.class)
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
    @ConditionalOnMissingBean(KeycloakUserGroupLookupProxy.class)
    public KeycloakUserGroupLookupProxy keycloakUserGroupLookupProxy(KeycloakLookupService keycloakLookupService){
        return new KeycloakUserGroupLookupProxy(keycloakLookupService);
    }

    @Bean
    @ConditionalOnMissingBean(KeycloakUserRoleLookupProxy.class)
    public KeycloakUserRoleLookupProxy keycloakUserRoleLookupProxy(KeycloakLookupService keycloakLookupService){
        return new KeycloakUserRoleLookupProxy(keycloakLookupService);
    }
}
