package org.activiti.cloud.identity.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

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
        boolean isIdpClient = clientRegistrationId.equals(template.feignTarget().name());
        if (isIdpClient) {
            OAuth2AccessToken accessToken = oAuth2AuthorizedClientManager.authorize(oAuth2AuthorizeRequest).getAccessToken();
            String authorizationToken = String.format("%s %s", accessToken.getTokenType().getValue(), accessToken.getTokenValue());
            template.header("Authorization", new String[]{authorizationToken});
        }
    }

}
