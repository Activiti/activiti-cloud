package org.activiti.cloud.security.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import feign.RequestTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@ExtendWith(MockitoExtension.class)
class ClientCredentialsAuthRequestInterceptorTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @Mock
    private ClientRegistration clientRegistration;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;
    @Mock
    private OAuth2AccessToken accessToken;

    @InjectMocks
    private ClientCredentialsAuthRequestInterceptor clientCredentialsAuthRequestInterceptor;

    @Test
    public void shouldSetAuthorizationHeader_whenClientCredentials() {
        when(clientRegistration.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.CLIENT_CREDENTIALS);
        when(clientRegistration.getRegistrationId()).thenReturn("keycloak");
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("123");

        RequestTemplate requestTemplate = new RequestTemplate();
        clientCredentialsAuthRequestInterceptor.apply(requestTemplate);
        assertThat(requestTemplate.headers()).containsEntry("Authorization", List.of("Bearer 123"));
    }

    @Test
    public void shouldSetOnlyOneAuthorizationHeader_whenClientCredentials() {
        when(clientRegistration.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.CLIENT_CREDENTIALS);
        when(clientRegistration.getRegistrationId()).thenReturn("keycloak");
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("123");

        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.header("Authorization", "Bearer 456");

        clientCredentialsAuthRequestInterceptor.apply(requestTemplate);
        assertThat(requestTemplate.headers()).hasSize(1);
        assertThat(requestTemplate.headers()).containsEntry("Authorization", List.of("Bearer 123"));
    }

    @Test
    public void shouldNotSetAnyHeader_whenIsNotClientCredentials() {
        when(clientRegistration.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.PASSWORD);

        RequestTemplate requestTemplate = new RequestTemplate();
        clientCredentialsAuthRequestInterceptor.apply(requestTemplate);
        assertThat(requestTemplate.headers()).isEmpty();
    }

}
