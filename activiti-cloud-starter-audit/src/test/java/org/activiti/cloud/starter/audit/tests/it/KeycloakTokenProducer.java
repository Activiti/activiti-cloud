package org.activiti.cloud.starter.audit.tests.it;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
        httpRequest.getHeaders().set(AUTHORIZATION_HEADER, getTokenString());
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }

    private AccessTokenResponse getAccessTokenResponse() {
        return Keycloak.getInstance(this.authServer, this.realm, this.keycloakTestUser, this.keycloakTestPassword, this.resource).tokenManager().getAccessToken();
    }

    private String getTokenString(){
        AccessTokenResponse token = getAccessTokenResponse();
        return "Bearer " + token.getToken();
    }

    public HttpEntity entityWithAuthorizationHeader() {
        HttpHeaders headers = authorizationHeaders();
        return new HttpEntity<>("parameters",
                                headers);
    }

    public HttpHeaders authorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER,
                    getTokenString());
        return headers;
    }
}
