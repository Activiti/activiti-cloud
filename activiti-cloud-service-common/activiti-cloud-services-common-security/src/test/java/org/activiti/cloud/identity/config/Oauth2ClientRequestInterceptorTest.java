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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.RequestTemplate;
import feign.Target.HardCodedTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;

@ExtendWith(MockitoExtension.class)
class Oauth2ClientRequestInterceptorTest {

    @Mock
    private RequestTemplate template;
    @Mock
    private OAuth2AccessToken accessToken;
    @Mock
    private OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    @Mock
    private OAuth2AuthorizeRequest oAuth2AuthorizeRequest;
    @Mock
    private OAuth2AuthorizedClient oAuth2AuthorizedClient;

    @Test
    void shouldSetAuthHeaderWhenClientRegistrationIdEqualsTFeignTarget() {
        String clientRegistrationId = "keycloak";
        Oauth2ClientRequestInterceptor oauth2ClientRequestInterceptor = new Oauth2ClientRequestInterceptor(clientRegistrationId, oAuth2AuthorizedClientManager, oAuth2AuthorizeRequest);

        when(template.feignTarget()).thenReturn(new HardCodedTarget(String.class, "keycloak", "http://"));
        when(oAuth2AuthorizedClientManager.authorize(oAuth2AuthorizeRequest)).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenType()).thenReturn(TokenType.BEARER);
        when(accessToken.getTokenValue()).thenReturn("123");

        oauth2ClientRequestInterceptor.apply(template);

        verify(template).header(eq("Authorization"), eq("Bearer 123"));
    }

    @Test
    void shouldNotSetAuthHeaderWhenClientRegistrationIdNotEqualsTFeignTarget() {
        String clientRegistrationId = "other";
        when(template.feignTarget()).thenReturn(new HardCodedTarget(String.class, "keycloak", "http://"));

        Oauth2ClientRequestInterceptor oauth2ClientRequestInterceptor = new Oauth2ClientRequestInterceptor(clientRegistrationId, oAuth2AuthorizedClientManager, oAuth2AuthorizeRequest);

        oauth2ClientRequestInterceptor.apply(template);

        verify(template, never()).header(anyString(), anyString());
    }

    @Test
    void shouldNotSetAuthHeaderWhenClientRegistrationIdIsNull() {
        String clientRegistrationId = null;
        when(template.feignTarget()).thenReturn(new HardCodedTarget(String.class, "keycloak", "http://"));

        Oauth2ClientRequestInterceptor oauth2ClientRequestInterceptor = new Oauth2ClientRequestInterceptor(clientRegistrationId, oAuth2AuthorizedClientManager, oAuth2AuthorizeRequest);

        oauth2ClientRequestInterceptor.apply(template);

        verify(template, never()).header(anyString(), anyString());
    }

    @Test
    void shouldNotSetAuthHeaderWhenClientRegistrationIdIsEmpty() {
        String clientRegistrationId = "";
        when(template.feignTarget()).thenReturn(new HardCodedTarget(String.class, "keycloak", "http://"));

        Oauth2ClientRequestInterceptor oauth2ClientRequestInterceptor = new Oauth2ClientRequestInterceptor(clientRegistrationId, oAuth2AuthorizedClientManager, oAuth2AuthorizeRequest);

        oauth2ClientRequestInterceptor.apply(template);

        verify(template, never()).header(anyString(), anyString());
    }
}
