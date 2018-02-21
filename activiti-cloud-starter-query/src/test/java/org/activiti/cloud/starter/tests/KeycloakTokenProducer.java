package org.activiti.cloud.starter.tests;

import java.io.IOException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class KeycloakTokenProducer implements ClientHttpRequestInterceptor {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    @Value("${keycloak.auth-server-url}")
    protected String authServer;
    @Value("${keycloak.realm}")
    protected String realm;
    @Value("${keycloak.resource}")
    protected String resource;
    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;
    @Value("${activiti.keycloak.test-password}")
    protected String keycloakTestPassword;

    public KeycloakTokenProducer() {
    }

    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        AccessTokenResponse token = this.getAccessTokenResponse();
        httpRequest.getHeaders().set("Authorization", "Bearer " + token.getToken());
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }

    public AccessTokenResponse getAccessTokenResponse() {
        return Keycloak.getInstance(this.authServer, this.realm, this.keycloakTestUser, this.keycloakTestPassword, this.resource).tokenManager().getAccessToken();
    }

    public String getTokenString(){
        AccessTokenResponse token = this.getAccessTokenResponse();
        return "Bearer " + token.getToken();
    }

    public void setKeycloakTestUser(String keycloakTestUser) {
        this.keycloakTestUser = keycloakTestUser;
    }

    public String getKeycloakTestUser() {
        return this.keycloakTestUser;
    }
}
