package org.activiti.cloud.services.common.security.keycloak.config;/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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


import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenPrincipalGroupsProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenPrincipalRolesProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenValidator;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalGroupsProviderChain;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalIdentityProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalRolesProviderChain;
import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityContextPrincipalProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityContextTokenProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityManagerImpl;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.List;

@Configuration
@KeycloakConfiguration
@ConditionalOnWebApplication
@ConditionalOnMissingBean(value = {KeycloakConfigResolver.class, SessionAuthenticationStrategy.class, SessionAuthenticationStrategy.class})
public class CommonSecurityAutoConfiguration extends KeycloakWebSecurityConfigurerAdapter {
    
    /**
     * Registers the KeycloakAuthenticationProvider with the authentication manager.
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth,
                                KeycloakAuthenticationProvider keycloakAuthenticationProvider) throws Exception {
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
        return new KeycloakAuthenticationProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextPrincipalProvider authenticatedPrincipalProvider() {
        return new KeycloakSecurityContextPrincipalProvider();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public KeycloakAccessTokenProvider keycloakAccessTokenProvider() {
        return new KeycloakAccessTokenProvider() { };
    }
    
    @Bean
    @ConditionalOnMissingBean
    public KeycloakAccessTokenValidator keycloakAccessTokenValidator() {
        return new KeycloakAccessTokenValidator() { };
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PrincipalIdentityProvider principalIdentityProvider(KeycloakAccessTokenProvider keycloakAccessTokenProvider,
                                                               KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
        return new KeycloakPrincipalIdentityProvider(keycloakAccessTokenProvider, 
                                                     keycloakAccessTokenValidator);
    }
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public KeycloakAccessTokenPrincipalGroupsProvider keycloakAccessTokenPrincipalGroupsProvider(KeycloakAccessTokenProvider keycloakAccessTokenProvider,
                                                                                                 KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
        return new KeycloakAccessTokenPrincipalGroupsProvider(keycloakAccessTokenProvider,
                                                              keycloakAccessTokenValidator);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public KeycloakAccessTokenPrincipalRolesProvider keycloakAccessTokenPrincipalRolesProvider(KeycloakAccessTokenProvider keycloakAccessTokenProvider,
                                                                                               KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
        return new KeycloakAccessTokenPrincipalRolesProvider(keycloakAccessTokenProvider,
                                                             keycloakAccessTokenValidator);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public KeycloakPrincipalGroupsProviderChain principalGroupsProviderChain(List<PrincipalGroupsProvider> principalGroupsProviders) {
        return new KeycloakPrincipalGroupsProviderChain(principalGroupsProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakPrincipalRolesProviderChain principalRolesProviderChain(List<PrincipalRolesProvider> principalRolesProviders) {
        return new KeycloakPrincipalRolesProviderChain(principalRolesProviders);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SecurityManager securityManager(SecurityContextPrincipalProvider authenticatedPrincipalProvider,
                                           PrincipalIdentityProvider principalIdentityProvider,
                                           KeycloakPrincipalGroupsProviderChain principalGroupsProvider,
                                           KeycloakPrincipalRolesProviderChain principalRolesProviderChain) {
        return new KeycloakSecurityManagerImpl(authenticatedPrincipalProvider,
                                               principalIdentityProvider,
                                               principalGroupsProvider,
                                               principalRolesProviderChain);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SecurityContextTokenProvider securityContextTokenProvider () {
        return new KeycloakSecurityContextTokenProvider();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public KeycloakConfigResolver KeycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    @Override
    @ConditionalOnMissingBean(HttpSessionManager.class)
    protected HttpSessionManager httpSessionManager() {
        return new HttpSessionManager();
    }


    /**
     * Defines the session authentication strategy.
     */
    @Bean
    @Override
    @ConditionalOnMissingBean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeRequests()
                .anyRequest().permitAll().and().csrf().disable().httpBasic().disable();
    }

}