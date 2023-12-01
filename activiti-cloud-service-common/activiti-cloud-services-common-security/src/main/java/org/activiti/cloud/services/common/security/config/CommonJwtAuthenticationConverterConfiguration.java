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

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.common.security.jwt.JwtGrantedAuthorityConverter;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.common.security.jwt.OAuth2UserServiceCacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
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
@EnableCaching
public class CommonJwtAuthenticationConverterConfiguration {

    @Value("${jwt.userinfo.cache.cacheExpireAfterWrite:PT10m}")
    private String cacheExpireAfterWrite;

    @Value("${jwt.userinfo.cache.cacheMaxSize:1000}")
    private int cacheMaxSize;

    private final OAuth2UserService oAuth2UserService;

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${activiti.cloud.services.oauth2.iam-name}")
    private String iamName;

    @Autowired
    public CommonJwtAuthenticationConverterConfiguration(ClientRegistrationRepository clientRegistrationRepository) {
        this.oAuth2UserService = new DefaultOAuth2UserService();
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean("commonJwtAuthenticationConverter")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
        JwtAccessTokenProvider jwtAccessTokenProvider,
        OAuth2UserServiceCacheable oAuth2UserServiceCacheable
    ) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(iamName);
        JwtGrantedAuthorityConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthorityConverter(
            jwtAccessTokenProvider
        );
        return new JwtUserInfoUriAuthenticationConverter(
            jwtGrantedAuthoritiesConverter,
            clientRegistration,
            oAuth2UserServiceCacheable
        );
    }

    @Bean
    public OAuth2UserServiceCacheable oAuth2UserServiceCacheable() {
        return new OAuth2UserServiceCacheable(oAuth2UserService);
    }

    @Bean
    public CaffeineCache userInfoApiCallCache() {
        return new CaffeineCache(
            "userInfoApiCall",
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.parse(cacheExpireAfterWrite))
                .maximumSize(cacheMaxSize)
                .build()
        );
    }

    public void setIamName(String iamName) {
        this.iamName = iamName;
    }
}
