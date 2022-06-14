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
package org.activiti.cloud.identity.web.controller.config;

import feign.RequestInterceptor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class Oauth2FeignConfiguration {

    @Value("${activiti.cloud.services.oauth2.iam-name}")
    private String clientRegistrationId;

    @Bean
    @ConditionalOnMissingBean({ClientRegistrationRepository.class})
    public InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        List<ClientRegistration> registrations = new ArrayList(OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    @ConditionalOnMissingBean({OAuth2AuthorizedClientService.class})
    public OAuth2AuthorizedClientService oauth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizedClientManager authorizedClientServiceAndManager(ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public RequestInterceptor requestInterceptor(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, OAuth2AuthorizeRequest oAuth2AuthorizeRequest) {
        return new Oauth2ClientRequestInterceptor(clientRegistrationId, oAuth2AuthorizedClientManager, oAuth2AuthorizeRequest);
    }

    @Bean
    public OAuth2AuthorizeRequest oAuth2AuthorizeRequest() {
        return OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistrationId)
            .principal("ActivitiCloud")
            .build();
    }

}
