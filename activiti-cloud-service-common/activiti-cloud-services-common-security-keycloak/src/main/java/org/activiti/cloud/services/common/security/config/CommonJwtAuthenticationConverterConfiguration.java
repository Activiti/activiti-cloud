package org.activiti.cloud.services.common.security.config;

import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.common.security.keycloak.KeycloakJwtGrantedAuthorityConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class CommonJwtAuthenticationConverterConfiguration {

    private final OAuth2UserService oAuth2UserService;

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${keycloak.resource}" )
    private String resource;

    @Value("${keycloak.use-resource-role-mappings:false}" )
    private boolean useResourceRoleMapping;

    @Autowired
    public CommonJwtAuthenticationConverterConfiguration(ClientRegistrationRepository clientRegistrationRepository) {
        this.oAuth2UserService = new DefaultOAuth2UserService();
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenProvider jwtAccessTokenProvider() {
        return new JwtAccessTokenProvider(resource, useResourceRoleMapping);
    }

    @Bean("commonJwtAuthenticationConverter")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("keycloak");
        KeycloakJwtGrantedAuthorityConverter jwtGrantedAuthoritiesConverter = new KeycloakJwtGrantedAuthorityConverter(jwtAccessTokenProvider());
        return new JwtUserInfoUriAuthenticationConverter(jwtGrantedAuthoritiesConverter, clientRegistration, oAuth2UserService);
    }
}
