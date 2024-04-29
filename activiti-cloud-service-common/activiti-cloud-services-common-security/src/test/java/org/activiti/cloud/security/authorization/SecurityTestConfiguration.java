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
package org.activiti.cloud.security.authorization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.activiti.cloud.services.common.security.config.CommonJwtAuthenticationConverterConfiguration;
import org.activiti.cloud.services.common.security.config.CommonSecurityAutoConfiguration;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtAdapter;
import org.activiti.cloud.services.common.security.jwt.JwtGrantedAuthorityConverter;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.common.security.jwt.OAuth2UserServiceCacheable;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@EnableWebSecurity
@SpringBootConfiguration
@Import({ CommonSecurityAutoConfiguration.class, CommonJwtAuthenticationConverterConfiguration.class })
@EnableConfigurationProperties(value = AuthorizationProperties.class)
public class SecurityTestConfiguration {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return mock(ClientRegistrationRepository.class);
    }

    @Bean
    public JwtAdapter jwtAdapter() {
        return mock(JwtAdapter.class);
    }

    @Bean
    public JwtAccessTokenProvider jwtAccessTokenProvider(JwtAdapter jwtAdapter) {
        return new JwtAccessTokenProvider(jwt -> jwtAdapter);
    }

    @Bean
    public Jwt jwt() {
        return mock(Jwt.class);
    }

    @Bean
    public JwtDecoder jwtDecoder(Jwt jwt) {
        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        when(jwtDecoder.decode(any())).thenReturn(jwt);
        return jwtDecoder;
    }

    @Bean
    public JwtGrantedAuthorityConverter jwtGrantedAuthorityConverter(JwtAccessTokenProvider jwtAccessTokenProvider) {
        return new JwtGrantedAuthorityConverter(jwtAccessTokenProvider);
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
        JwtGrantedAuthorityConverter jwtGrantedAuthorityConverter,
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2UserServiceCacheable oAuth2UserServiceCacheable
    ) {
        JwtUserInfoUriAuthenticationConverter converter = new JwtUserInfoUriAuthenticationConverter(
            jwtGrantedAuthorityConverter,
            clientRegistrationRepository.findByRegistrationId("keycloak"),
            oAuth2UserServiceCacheable
        );
        JwtUserInfoUriAuthenticationConverter spy = spy(converter);
        doReturn("test").when(spy).getPrincipalClaimName(any(Jwt.class));
        return spy;
    }
}
