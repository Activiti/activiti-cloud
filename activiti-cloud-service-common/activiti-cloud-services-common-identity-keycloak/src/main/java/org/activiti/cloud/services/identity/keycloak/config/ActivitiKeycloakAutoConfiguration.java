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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.activiti.cloud.identity.IdentityManagementService;
import org.activiti.cloud.services.identity.keycloak.ActivitiKeycloakProperties;
import org.activiti.cloud.services.identity.keycloak.KeycloakClientPrincipalDetailsProvider;
import org.activiti.cloud.services.identity.keycloak.KeycloakHealthService;
import org.activiti.cloud.services.identity.keycloak.KeycloakManagementService;
import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.activiti.cloud.services.identity.keycloak.KeycloakUserGroupManager;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.validator.PublicKeyValidationCheck;
import org.activiti.cloud.services.identity.keycloak.validator.RealmValidationCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@PropertySource("classpath:keycloak-client.properties")
@ConditionalOnProperty(value = "activiti.cloud.services.oauth2.iam-name", havingValue = "keycloak", matchIfMissing = true)
@EnableConfigurationProperties({ActivitiKeycloakProperties.class, KeycloakProperties.class})
@EnableFeignClients(clients = KeycloakClient.class)
public class ActivitiKeycloakAutoConfiguration {

    @Value("${identity.client.cache.cacheExpireAfterWrite:PT5m}")
    private String cacheExpireAfterWrite;
    @Value("${identity.client.cache.cacheMaxSize:1000}")
    private int cacheMaxSize;


    @Bean(name = "userGroupManager")
    @ConditionalOnMissingBean(KeycloakUserGroupManager.class)
    public KeycloakUserGroupManager keycloakUserGroupManager(KeycloakClient keycloakClient) {
        return new KeycloakUserGroupManager(keycloakClient);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public KeycloakClientPrincipalDetailsProvider keycloakClientPrincipalDetailsProvider(KeycloakClient keycloakClient) {
        return new KeycloakClientPrincipalDetailsProvider(keycloakClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdentityManagementService identityManagementService(KeycloakClient keycloakClient) {
        return new KeycloakManagementService(keycloakClient);
    }

    @Bean
    public CaffeineCache groupRoleMappingCache() {
        return new CaffeineCache("groupRoleMapping",
                                 Caffeine.newBuilder()
                                     .expireAfterWrite(Duration.parse(cacheExpireAfterWrite))
                                     .maximumSize(cacheMaxSize)
                                     .build());
    }

    @Bean
    public CaffeineCache userRoleMappingCache() {
        return new CaffeineCache("userRoleMapping",
                                 Caffeine.newBuilder()
                                     .expireAfterWrite(Duration.parse(cacheExpireAfterWrite))
                                     .maximumSize(cacheMaxSize)
                                     .build());
    }

    @Bean
    public CaffeineCache userGroupsCache() {
        return new CaffeineCache("userGroups",
                                 Caffeine.newBuilder()
                                     .expireAfterWrite(Duration.parse(cacheExpireAfterWrite))
                                     .maximumSize(cacheMaxSize)
                                     .build());
    }

    @Bean(name = "identityHealthService")
    @ConditionalOnMissingBean(KeycloakHealthService.class)
    public KeycloakHealthService keycloakHealthService(KeycloakUserGroupManager keycloakUserGroupManager) {
        return new KeycloakHealthService(keycloakUserGroupManager);
    }

    @Bean
    public PublicKeyValidationCheck publicKeyValidationCheck(@Value("${keycloak.auth-server-url}") String authServerUrl,
                                                    @Value("${keycloak.realm}") String realm,
                                                    ObjectMapper objectMapper) {
        return new PublicKeyValidationCheck(authServerUrl, realm, objectMapper);
    }

    @Bean
    public RealmValidationCheck realmValidationCheck(@Value("${keycloak.auth-server-url}") String authServerUrl,
                                                @Value("${keycloak.realm}") String realm) {
        return new RealmValidationCheck(authServerUrl, realm);
    }

}
