/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import feign.RequestInterceptor;
import java.util.List;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.security.authorization.AuthorizationConfigurer;
import org.activiti.cloud.security.authorization.EnableAuthorizationConfiguration;
import org.activiti.cloud.security.authorization.PublicUrlMatcher;
import org.activiti.cloud.security.feign.TokenRelayRequestInterceptor;
import org.activiti.cloud.services.common.security.CustomBearerTokenAccessDeniedHandler;
import org.activiti.cloud.services.common.security.SecurityManagerImpl;
import org.activiti.cloud.services.common.security.jwt.JtwAccessTokenPrincipalRolesProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenPrincipalGroupsProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalGroupsProviderChain;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalIdentityProvider;
import org.activiti.cloud.services.common.security.jwt.JwtPrincipalRolesProviderChain;
import org.activiti.cloud.services.common.security.jwt.JwtSecurityContextPrincipalProvider;
import org.activiti.cloud.services.common.security.jwt.JwtSecurityContextTokenProvider;
import org.activiti.cloud.services.common.security.jwt.validator.ExpiredValidationCheck;
import org.activiti.cloud.services.common.security.jwt.validator.IsNotBeforeValidationCheck;
import org.activiti.cloud.services.common.security.jwt.validator.ValidationCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.cors.CorsConfiguration;

@AutoConfiguration
@EnableAuthorizationConfiguration
@ConditionalOnWebApplication
@ConditionalOnMissingBean(value = { SessionAuthenticationStrategy.class, SessionAuthenticationStrategy.class })
@Import(CommonJwtAuthenticationConverterConfiguration.class)
@PropertySource("classpath:config/activiti-cloud-services-common-security.properties")
public class CommonSecurityAutoConfiguration {

    private final AuthorizationConfigurer authorizationConfigurer;

    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Value("${authorization.validation.offset:0}")
    private long offset;

    @Value("${cors.allowedOrigins:*}")
    private List<String> allowedOrigins;

    private final List<HttpSecurityCustomizer> httpSecurityCustomizers;

    @Autowired
    public CommonSecurityAutoConfiguration(
        AuthorizationConfigurer authorizationConfigurer,
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
        @Autowired(required = false) List<HttpSecurityCustomizer> httpSecurityCustomizers
    ) {
        this.authorizationConfigurer = authorizationConfigurer;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.httpSecurityCustomizers = httpSecurityCustomizers != null ? httpSecurityCustomizers : List.of();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextPrincipalProvider authenticatedPrincipalProvider() {
        return new JwtSecurityContextPrincipalProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenValidator jwtAccessTokenValidator(List<ValidationCheck> validationChecks) {
        return new JwtAccessTokenValidator(validationChecks);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpiredValidationCheck expiredValidationCheck() {
        return new ExpiredValidationCheck(offset);
    }

    @Bean
    @ConditionalOnMissingBean
    public IsNotBeforeValidationCheck isNotBeforeValidationCheck() {
        return new IsNotBeforeValidationCheck(offset);
    }

    @Bean
    @ConditionalOnMissingBean
    public PrincipalIdentityProvider principalIdentityProvider(
        JwtAccessTokenProvider jwtAccessTokenProvider,
        JwtAccessTokenValidator jwtAccessTokenValidator
    ) {
        return new JwtPrincipalIdentityProvider(jwtAccessTokenProvider, jwtAccessTokenValidator);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public JwtAccessTokenPrincipalGroupsProvider jwtAccessTokenPrincipalGroupsProvider(
        JwtAccessTokenProvider jwtAccessTokenProvider,
        JwtAccessTokenValidator jwtAccessTokenValidator
    ) {
        return new JwtAccessTokenPrincipalGroupsProvider(jwtAccessTokenProvider, jwtAccessTokenValidator);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public JtwAccessTokenPrincipalRolesProvider jtwAccessTokenPrincipalRolesProvider(
        JwtAccessTokenProvider jwtAccessTokenProvider,
        JwtAccessTokenValidator jwtAccessTokenValidator
    ) {
        return new JtwAccessTokenPrincipalRolesProvider(jwtAccessTokenProvider, jwtAccessTokenValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtPrincipalGroupsProviderChain principalGroupsProviderChain(
        List<PrincipalGroupsProvider> principalGroupsProviders
    ) {
        return new JwtPrincipalGroupsProviderChain(principalGroupsProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtPrincipalRolesProviderChain principalRolesProviderChain(
        List<PrincipalRolesProvider> principalRolesProviders
    ) {
        return new JwtPrincipalRolesProviderChain(principalRolesProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityManager securityManager(
        SecurityContextPrincipalProvider authenticatedPrincipalProvider,
        PrincipalIdentityProvider principalIdentityProvider,
        JwtPrincipalGroupsProviderChain principalGroupsProvider,
        JwtPrincipalRolesProviderChain principalRolesProviderChain
    ) {
        return new SecurityManagerImpl(
            authenticatedPrincipalProvider,
            principalIdentityProvider,
            principalGroupsProvider,
            principalRolesProviderChain
        );
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

    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor tokenRelayRequestInterceptor(SecurityContextTokenProvider securityContextTokenProvider) {
        return new TokenRelayRequestInterceptor(securityContextTokenProvider);
    }

    /**
     * Provides a {@link BearerTokenResolver} that skips bearer-token extraction on
     * <em>unconditionally</em> public URLs — i.e. security constraints without any
     * required role or permission that are also not shadowed by another restricted
     * constraint. When the resolver returns {@code null}, Spring Security's
     * {@code BearerTokenAuthenticationFilter} bypasses authentication entirely for
     * the current request, so a stale/expired {@code Authorization} header on a
     * truly public endpoint will not produce a 401.
     *
     * <p>Patterns that are declared as public but are also matched by a restricted
     * constraint (public "overrides", typically used to switch off the HTTP-level
     * role check while delegating authorization to method-level annotations such
     * as {@code @PreAuthorize}) are intentionally excluded here: for those the
     * bearer token must be extracted and validated, otherwise the
     * {@code SecurityContext} would not carry the authorities the method-level
     * check needs.
     *
     * @see AuthorizationConfigurer#getStrictlyPublicUrlPatterns()
     */
    @Bean
    @ConditionalOnMissingBean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        List<String> publicUrls = authorizationConfigurer.getStrictlyPublicUrlPatterns();
        if (publicUrls.isEmpty()) {
            return delegate;
        }
        PublicUrlMatcher publicUrlMatcher = new PublicUrlMatcher(publicUrls);
        return request -> publicUrlMatcher.matches(request) ? null : delegate.resolve(request);
    }

    @Bean
    @SuppressWarnings({ "java:S4502", "java:S5122" })
    public SecurityFilterChain filterChain(HttpSecurity http, BearerTokenResolver bearerTokenResolver)
        throws Exception {
        authorizationConfigurer.configure(http);
        http
            .authorizeHttpRequests(spec ->
                spec.requestMatchers(actuatorEndpointsMatcher()).authenticated().anyRequest().permitAll()
            )
            .cors(spec ->
                spec.configurationSource(request -> {
                    CorsConfiguration corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedMethods(List.of("GET", "HEAD", "OPTION", "POST", "PUT", "DELETE"));
                    corsConfiguration.setAllowedOrigins(allowedOrigins);
                    return corsConfiguration.applyPermitDefaultValues();
                })
            )
            .exceptionHandling(spec ->
                spec.accessDeniedHandler(new CustomBearerTokenAccessDeniedHandler(new BearerTokenAccessDeniedHandler()))
            )
            .httpBasic(spec -> spec.disable())
            .oauth2ResourceServer(spec ->
                spec
                    .bearerTokenResolver(bearerTokenResolver)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            );

        for (HttpSecurityCustomizer customizer : httpSecurityCustomizers) {
            customizer.customize(http);
        }

        return http.build();
    }

    private RequestMatcher actuatorEndpointsMatcher() {
        RequestMatcher actuatorMatcher = PathPatternRequestMatcher.pathPattern("/actuator/**");
        RequestMatcher healthMatcher = PathPatternRequestMatcher.pathPattern("/actuator/health/**");
        RequestMatcher infoMatcher = PathPatternRequestMatcher.pathPattern("/actuator/info/**");

        RequestMatcher excludeMatcher = RequestMatchers.anyOf(healthMatcher, infoMatcher);

        return request -> actuatorMatcher.matches(request) && !excludeMatcher.matches(request);
    }
}
