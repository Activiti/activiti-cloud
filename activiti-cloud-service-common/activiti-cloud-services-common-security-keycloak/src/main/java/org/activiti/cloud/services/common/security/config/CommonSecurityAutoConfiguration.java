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
package org.activiti.cloud.services.common.security.config;

import java.util.List;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.security.authorization.AuthorizationConfigurer;
import org.activiti.cloud.security.authorization.EnableAuthorizationConfiguration;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenPrincipalGroupsProvider;
import org.activiti.cloud.services.common.security.jwt.JtwAccessTokenPrincipalRolesProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalGroupsProviderChain;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalIdentityProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalRolesProviderChain;
import org.activiti.cloud.services.common.security.jwt.JwtSecurityContextPrincipalProvider;
import org.activiti.cloud.services.common.security.jwt.JwtSecurityContextTokenProvider;
import org.activiti.cloud.services.common.security.SecurityManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableAuthorizationConfiguration
@ConditionalOnWebApplication
@ConditionalOnMissingBean(value = {SessionAuthenticationStrategy.class, SessionAuthenticationStrategy.class})
@PropertySource("classpath:keycloak-configuration.properties" )
@Import(CommonJwtAuthenticationConverterConfiguration.class)
public class CommonSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    private final AuthorizationConfigurer authorizationConfigurer;

    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Value("${authorization.validation.offset:0}" )
    private long offset;

    @Value("${cors.allowedOrigins:*}" )
    private List<String> allowedOrigins;

    @Autowired
    public CommonSecurityAutoConfiguration(AuthorizationConfigurer authorizationConfigurer,
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) {
        this.authorizationConfigurer = authorizationConfigurer;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextPrincipalProvider authenticatedPrincipalProvider() {
        return new JwtSecurityContextPrincipalProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenValidator keycloakAccessTokenValidator() {
        return new JwtAccessTokenValidator(offset);
    }

    @Bean
    @ConditionalOnMissingBean
    public PrincipalIdentityProvider principalIdentityProvider(JwtAccessTokenProvider keycloakAccessTokenProvider,
                                                               JwtAccessTokenValidator jwtAccessTokenValidator) {
        return new JwtPrincipalIdentityProvider(keycloakAccessTokenProvider,
            jwtAccessTokenValidator);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public JwtAccessTokenPrincipalGroupsProvider keycloakAccessTokenPrincipalGroupsProvider(JwtAccessTokenProvider jwtAccessTokenProvider,
        JwtAccessTokenValidator jwtAccessTokenValidator) {
        return new JwtAccessTokenPrincipalGroupsProvider(jwtAccessTokenProvider,
            jwtAccessTokenValidator);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public JtwAccessTokenPrincipalRolesProvider keycloakAccessTokenPrincipalRolesProvider(JwtAccessTokenProvider keycloakAccessTokenProvider,
                                                                                               JwtAccessTokenValidator jwtAccessTokenValidator) {
        return new JtwAccessTokenPrincipalRolesProvider(keycloakAccessTokenProvider,
            jwtAccessTokenValidator);
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
        return new SecurityManagerImpl(authenticatedPrincipalProvider,
                                               principalIdentityProvider,
                                               principalGroupsProvider,
                                               principalRolesProviderChain);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextTokenProvider securityContextTokenProvider() {
        return new JwtSecurityContextTokenProvider();
    }

    /**
     * Defines the session authentication strategy.
     */
    @Bean
    @ConditionalOnMissingBean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        authorizationConfigurer.configure(http);
        http
            .authorizeRequests().anyRequest().permitAll()
            .and()
            .cors()
            .configurationSource(request -> {
                CorsConfiguration corsConfiguration = new CorsConfiguration();
                corsConfiguration.setAllowedMethods(List.of("GET", "HEAD", "OPTION", "POST", "PUT", "DELETE"));
                corsConfiguration.setAllowedOrigins(allowedOrigins);
                return corsConfiguration.applyPermitDefaultValues();
            })
            .and()
            .csrf().disable()
            .httpBasic().disable()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(jwtAuthenticationConverter);
    }

}
