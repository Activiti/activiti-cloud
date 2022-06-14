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
import feign.RequestTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.CollectionUtils;

public class Oauth2ClientRequestInterceptor implements RequestInterceptor {


    private String clientRegistrationId;
    private OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    private OAuth2AuthorizeRequest oAuth2AuthorizeRequest;

    public Oauth2ClientRequestInterceptor(String clientRegistrationId,
                                          OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
                                          OAuth2AuthorizeRequest oAuth2AuthorizeRequest) {
        this.clientRegistrationId = clientRegistrationId;
        this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
        this.oAuth2AuthorizeRequest = oAuth2AuthorizeRequest;
    }

    @Override
    public void apply(RequestTemplate template) {
        boolean isIdpClient = template.feignTarget().name().equals(clientRegistrationId);
        if (isIdpClient && hasNotAuthorizationHeader(template)) {
            OAuth2AccessToken accessToken = oAuth2AuthorizedClientManager.authorize(oAuth2AuthorizeRequest).getAccessToken();
            String authorizationToken = String.format("%s %s", accessToken.getTokenType().getValue(), accessToken.getTokenValue());
            template.header("Authorization", authorizationToken);
        }
    }

    /**
     * Check if the authorization header it is already propagated.
     * Other interceptors like org.activiti.cloud.security.feign.TokenRelayRequestInterceptor could do this
     * @param template
     * @return true if the header doesn't contain an Authorization property
     */
    private boolean hasNotAuthorizationHeader(RequestTemplate template) {
        return CollectionUtils.isEmpty(template.headers().get("Authorization"));
    }

}
