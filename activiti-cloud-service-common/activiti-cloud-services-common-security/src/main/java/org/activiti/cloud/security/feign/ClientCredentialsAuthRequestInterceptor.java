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
package org.activiti.cloud.security.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 * Feign request interceptor for forwarding the bearer token
 */
public class ClientCredentialsAuthRequestInterceptor implements RequestInterceptor {

    public static final String BEARER = "Bearer";

    public static final String AUTHORIZATION = "Authorization";
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ClientRegistration clientRegistration;


    public ClientCredentialsAuthRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager,
                                                   ClientRegistration clientRegistration) {
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistration = clientRegistration;
    }

    @Override
    public void apply(RequestTemplate template) {
        if(AuthorizationGrantType.CLIENT_CREDENTIALS.equals(clientRegistration.getAuthorizationGrantType())) {
            OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistration.getRegistrationId())
                .principal("activiti")
                .build();

            OAuth2AuthorizedClient client = authorizedClientManager.authorize(oAuth2AuthorizeRequest);
            OAuth2AccessToken accessToken = client.getAccessToken();

            template.removeHeader(AUTHORIZATION);
            template.header(AUTHORIZATION,
                            String.format("%s %s",
                                          accessToken.getTokenType().getValue(),
                                          accessToken.getTokenValue()));
        }
    }
}
