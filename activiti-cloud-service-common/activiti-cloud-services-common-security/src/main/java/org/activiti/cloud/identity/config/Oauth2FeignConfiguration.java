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
package org.activiti.cloud.identity.config;

import feign.RequestInterceptor;
import java.util.Collection;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Configuration
public class Oauth2FeignConfiguration {

    @Bean
    public AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager(ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public RequestInterceptor requestInterceptor(Supplier<OAuth2AccessToken> accessTokenSupplier) {
        return template -> {Collection<String> authorization = template.headers().get("Authorization");
            if (CollectionUtils.isEmpty(authorization)  || !StringUtils.hasText(authorization.iterator().next())) {
                OAuth2AccessToken accessToken = accessTokenSupplier.get();
                template.header("Authorization", "Bearer " + accessToken.getTokenValue());
            }
        };
    }

    @Bean("accessTokenSupplier")
    public Supplier<OAuth2AccessToken> accessTokenSupplier(@Value("${activiti.cloud.services.oauth2.client-registration-id}") String clientRegistrationId, OAuth2AuthorizedClientManager authorizedClientManager) {
        return () -> {
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal("Activiti") //doesn't matter the value, but it is mandatory for Spring
                .build();
            return authorizedClientManager.authorize(request).getAccessToken();
        };
    }

}
