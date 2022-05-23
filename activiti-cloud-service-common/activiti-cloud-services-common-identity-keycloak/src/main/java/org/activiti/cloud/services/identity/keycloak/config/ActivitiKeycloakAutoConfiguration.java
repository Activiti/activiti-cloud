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
package org.activiti.cloud.services.identity.keycloak.config;

import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.services.identity.keycloak.ActivitiKeycloakProperties;
import org.activiti.cloud.services.identity.keycloak.KeycloakClientPrincipalDetailsProvider;
import org.activiti.cloud.services.identity.keycloak.KeycloakInstanceWrapper;
import org.activiti.cloud.services.identity.keycloak.KeycloakManagementService;
import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.activiti.cloud.services.identity.keycloak.KeycloakUserGroupManager;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakGroupToGroup;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakRoleMappingToRole;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakTokenToUserRoles;
import org.activiti.cloud.services.identity.keycloak.mapper.KeycloakUserToUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@PropertySource("classpath:keycloak-client.properties")
@ConditionalOnProperty(name = "activiti.cloud.services.keycloak.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ActivitiKeycloakProperties.class, KeycloakProperties.class})
@EnableFeignClients(clients = KeycloakClient.class)
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

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public KeycloakClientPrincipalDetailsProvider keycloakClientPrincipalDetailsProvider(KeycloakInstanceWrapper keycloakInstanceWrapper) {
        return new KeycloakClientPrincipalDetailsProvider(keycloakInstanceWrapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdentityManagementService identityManagementService(KeycloakClient keycloakClient,
                                                               KeycloakUserToUser keycloakUserToUser,
                                                               KeycloakGroupToGroup keycloakGroupToGroup,
                                                               KeycloakTokenToUserRoles keycloakTokenToUserRoles) {
        return new KeycloakManagementService(keycloakClient, keycloakUserToUser, keycloakGroupToGroup,
            keycloakTokenToUserRoles);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakUserToUser keycloakUserToUser(KeycloakClient keycloakClient, KeycloakRoleMappingToRole keycloakRoleMappingToRole) {
        return new KeycloakUserToUser(keycloakClient, keycloakRoleMappingToRole);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakRoleMappingToRole keycloakRoleMappingToRole() {
        return new KeycloakRoleMappingToRole();
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakGroupToGroup keycloakGroupToGroup(KeycloakClient keycloakClient, KeycloakRoleMappingToRole keycloakRoleMappingToRole) {
        return new KeycloakGroupToGroup(keycloakClient, keycloakRoleMappingToRole);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakTokenToUserRoles keycloakTokenToUserRoles() {
        return new KeycloakTokenToUserRoles();
    }

}
