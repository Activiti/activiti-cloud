package org.activiti.cloud.services.identity.keycloak.config;

import org.activiti.cloud.services.identity.keycloak.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.keycloak.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ActivitiKeycloakProperties.class, KeycloakProperties.class})
public class ActivitiKeycloakAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(KeycloakInstanceWrapper.class)
    public KeycloakInstanceWrapper keycloakInstanceWrapper() {
        return new KeycloakInstanceWrapper();
    }



    @Bean(name = "userGroupManager")
    @ConditionalOnMissingBean(KeycloakUserGroupManager.class)
    public KeycloakUserGroupManager keycloakUserGroupManager(KeycloakInstanceWrapper keycloakInstanceWrapper) {
        return new KeycloakUserGroupManager(keycloakInstanceWrapper);
    }

}
