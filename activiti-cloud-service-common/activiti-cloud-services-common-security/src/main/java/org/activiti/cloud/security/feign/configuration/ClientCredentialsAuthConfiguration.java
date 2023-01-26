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
package org.activiti.cloud.security.feign.configuration;

import org.activiti.cloud.security.feign.ClientCredentialsAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
//@ConditionalOnBean(OAuth2AuthorizedClientService.class)
public class ClientCredentialsAuthConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ClientCredentialsAuthRequestInterceptor clientCredentialsAuthRequestInterceptor(OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
                                                                                           ClientRegistrationRepository clientRegistrationRepository,
                                                                                           ClientRegistration clientRegistration) {

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService);


        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .refreshToken()
                .clientCredentials()
                .build();

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return new ClientCredentialsAuthRequestInterceptor(authorizedClientManager, clientRegistration);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientRegistration clientRegistration(ClientRegistrationRepository clientRegistrationRepository,
                                                 @Value("${activiti.cloud.services.oauth2.iam-name:keycloak}") String clientName) {
        return clientRegistrationRepository.findByRegistrationId(clientName);
    }

}
