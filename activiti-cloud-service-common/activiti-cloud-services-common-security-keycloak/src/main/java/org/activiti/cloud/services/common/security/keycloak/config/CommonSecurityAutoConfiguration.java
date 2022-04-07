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

import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.security.authorization.AuthorizationConfigurer;
import org.activiti.cloud.security.authorization.EnableAuthorizationConfiguration;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenValidator;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalGroupsProviderChain;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalRolesProviderChain;
import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityContextTokenProvider;
import org.activiti.cloud.services.common.security.oidc.OAuth2PrincipalGroupsProvider;
import org.activiti.cloud.services.common.security.oidc.OAuth2PrincipalIdentityProvider;
import org.activiti.cloud.services.common.security.oidc.OAuth2PrincipalRolesProvider;
import org.activiti.cloud.services.common.security.oidc.OAuth2SecurityContextPrincipalProvider;
import org.activiti.cloud.services.common.security.oidc.OAuth2SecurityManagerImpl;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultJwtBearerTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.JwtBearerGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.List;

@Configuration
//@KeycloakConfiguration
@EnableAuthorizationConfiguration
@EnableWebSecurity
@EnableConfigurationProperties({OAuth2ClientProperties.class})

@ConditionalOnWebApplication
@Import({KeycloakSpringBootConfigResolver.class})
@ConditionalOnMissingBean(value = {KeycloakConfigResolver.class, SessionAuthenticationStrategy.class, SessionAuthenticationStrategy.class})
@DependsOn({"keycloakConfigResolver"})
@PropertySource("classpath:keycloak-configuration.properties")
public class CommonSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    private final AuthorizationConfigurer authorizationConfigurer;

    private static final String OAUTH2_CLIENT_REGISTRATION_ID = "keycloak-client";

    @Autowired
    public CommonSecurityAutoConfiguration(AuthorizationConfigurer authorizationConfigurer) {
        this.authorizationConfigurer = authorizationConfigurer;
    }

//    /**
//     * Registers the KeycloakAuthenticationProvider with the authentication manager.
//     */
//    @Bean
//    //OAuth2LoginAuthenticationProvider needs to be provided instead of KeycloakAuthenticationProvider
//    public InitializingBean configureGlobalAuthenticationManager(AuthenticationManagerBuilder auth,
//        OAuth2LoginAuthenticationProvider oAuth2LoginAuthenticationProvider) {
//        return () -> {
//            oAuth2LoginAuthenticationProvider.setAuthoritiesMapper(new SimpleAuthorityMapper());
//            auth.authenticationProvider(oAuth2LoginAuthenticationProvider);
//        };
//    }

//    @Bean
//    public OAuth2AuthorizeRequest oAuth2AuthorizeRequest() {
//        return OAuth2AuthorizeRequest.withClientRegistrationId(OAUTH2_CLIENT_REGISTRATION_ID)
////            .principal(new AnonymousAuthenticationToken("feignClient", "feignClient",
////                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
////            .attribute(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, oAuth2Username)
////            .attribute(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, oAuth2Password)
//            .build();
//    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(this.oAuth2LoginAuthenticationProvider(new DefaultAuthorizationCodeTokenResponseClient(),
            new DefaultOAuth2UserService()));
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

//    @Bean
//    @ConditionalOnMissingBean
////    @Override
//    public KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
//        return new KeycloakAuthenticationProvider();
//    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2LoginAuthenticationProvider oAuth2LoginAuthenticationProvider(OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
        OAuth2UserService<OAuth2UserRequest, OAuth2User> userService){
        return new OAuth2LoginAuthenticationProvider(accessTokenResponseClient,userService );
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oAuth2AuthorizationCodeGrantRequestOAuth2AccessTokenResponseClient() {
        return new DefaultAuthorizationCodeTokenResponseClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> userService() {
        return new DefaultOAuth2UserService();
    }



//    @Bean
//    @ConditionalOnMissingBean
//    public SecurityContextPrincipalProvider authenticatedPrincipalProvider() {
//        return new KeycloakSecurityContextPrincipalProvider();
//    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextPrincipalProvider authenticatedPrincipalProvider() {
        return new OAuth2SecurityContextPrincipalProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakAccessTokenProvider keycloakAccessTokenProvider() {
        return new KeycloakAccessTokenProvider() {
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakAccessTokenValidator keycloakAccessTokenValidator() {
        return new KeycloakAccessTokenValidator() {
        };
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public PrincipalIdentityProvider principalIdentityProvider(KeycloakAccessTokenProvider keycloakAccessTokenProvider,
//        KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
//        return new KeycloakPrincipalIdentityProvider(keycloakAccessTokenProvider,
//            keycloakAccessTokenValidator);
//    }

    @Bean
    @ConditionalOnMissingBean
    public PrincipalIdentityProvider principalIdentityProvider() {
        return new OAuth2PrincipalIdentityProvider();
    }

//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE)
//    @ConditionalOnMissingBean
//    public KeycloakAccessTokenPrincipalGroupsProvider keycloakAccessTokenPrincipalGroupsProvider(KeycloakAccessTokenProvider keycloakAccessTokenProvider,
//        KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
//        return new KeycloakAccessTokenPrincipalGroupsProvider(keycloakAccessTokenProvider,
//            keycloakAccessTokenValidator);
//    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public OAuth2PrincipalGroupsProvider oAuth2PrincipalGroupsProvider() {
        return new OAuth2PrincipalGroupsProvider();
    }

//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE)
//    @ConditionalOnMissingBean
//    public KeycloakAccessTokenPrincipalRolesProvider keycloakAccessTokenPrincipalRolesProvider(KeycloakAccessTokenProvider keycloakAccessTokenProvider,
//        KeycloakAccessTokenValidator keycloakAccessTokenValidator) {
//        return new KeycloakAccessTokenPrincipalRolesProvider(keycloakAccessTokenProvider,
//            keycloakAccessTokenValidator);
//    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public PrincipalRolesProvider principalRolesProvider() {
        return new OAuth2PrincipalRolesProvider();
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

//    @Bean
//    @ConditionalOnMissingBean
//    public SecurityManager securityManager(SecurityContextPrincipalProvider authenticatedPrincipalProvider,
//        PrincipalIdentityProvider principalIdentityProvider,
//        KeycloakPrincipalGroupsProviderChain principalGroupsProvider,
//        KeycloakPrincipalRolesProviderChain principalRolesProviderChain) {
//        return new KeycloakSecurityManagerImpl(authenticatedPrincipalProvider,
//            principalIdentityProvider,
//            principalGroupsProvider,
//            principalRolesProviderChain);
//    }

    //OAUTH2 SECURITY MANAGER
    @Bean
    @ConditionalOnMissingBean
    public SecurityManager securityManager(SecurityContextPrincipalProvider authenticatedPrincipalProvider,
        OAuth2PrincipalIdentityProvider principalIdentityProvider,
        OAuth2PrincipalGroupsProvider principalGroupsProvider,
        OAuth2PrincipalRolesProvider principalRolesProviderChain) {
        return new OAuth2SecurityManagerImpl(authenticatedPrincipalProvider,
            principalIdentityProvider,
            principalGroupsProvider,
            principalRolesProviderChain);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextTokenProvider securityContextTokenProvider() {
        return new KeycloakSecurityContextTokenProvider();
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public SecurityContextTokenProvider securityContextTokenProvider(OAuth2AuthorizedClientService auth2AuthorizedClientService) {
//        return new OAuth2SecurityContextTokenProvider(auth2AuthorizedClientService);
//    }

    @Bean
//    @Override
    @ConditionalOnMissingBean(HttpSessionManager.class)
    protected HttpSessionManager httpSessionManager() {
        return new HttpSessionManager();
    }


    /**
     * Defines the session authentication strategy.
     */
    @Bean
//    @Override
    @ConditionalOnMissingBean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        super.configure(http);
        authorizationConfigurer.configure(http);

        http.antMatcher("/**")
            .authorizeRequests()
            .antMatchers("/")
            .permitAll()
            .anyRequest()
            .authenticated()
            .and()
            .oauth2Login()
            .tokenEndpoint()
            .accessTokenResponseClient(oAuth2AuthorizationCodeGrantRequestOAuth2AccessTokenResponseClient());

//        http
//            .csrf().disable()
//            .anonymous().disable()
//            .authorizeRequests()
//            .antMatchers("/oauth/token").permitAll();

//        http.authorizeRequests()
//            .anyRequest().permitAll()
//            .and()
//            .csrf().disable()
//            .httpBasic().disable();
//            .oauth2Login();


//        http.authorizeRequests()
//            .anyRequest().authenticated()
//            .and()
//            .oauth2Login();
    }

//    @Bean
//    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
//
//        return new NimbusAuthorizationCodeTokenResponseClient();
//    }

}
