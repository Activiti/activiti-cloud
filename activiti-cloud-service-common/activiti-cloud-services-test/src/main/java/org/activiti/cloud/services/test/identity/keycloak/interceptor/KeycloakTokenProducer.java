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
package org.activiti.cloud.services.test.identity.keycloak.interceptor;

import java.io.IOException;

import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class KeycloakTokenProducer implements ClientHttpRequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private KeycloakProperties keycloakProperties;

    @Value("${keycloak.resource:}")
    protected String resource;

    @Value("${activiti.identity.test-user:}")
    protected String keycloakTestUser;

    @Value("${activiti.identity.test-password:}")
    protected String keycloakTestPassword;

    public KeycloakTokenProducer(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest,
                                        byte[] bytes,
                                        ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }

    private AccessTokenResponse getAccessTokenResponse(String user, String password) {
        return Keycloak.getInstance(keycloakProperties.getAuthServerUrl(),
                                    keycloakProperties.getRealm(),
                                    user,
                                    password,
                                    resource).tokenManager().getAccessToken();
    }

    public String getTokenString(String user, String password) {
        AccessTokenResponse token = getAccessTokenResponse(user, password);
        return "Bearer " + token.getToken();
    }

    public HttpEntity entityWithAuthorizationHeader() {
        return this.entityWithAuthorizationHeader(keycloakTestUser, keycloakTestPassword);
    }

    public HttpEntity entityWithAuthorizationHeader(String user, String password) {
        HttpHeaders headers = authorizationHeaders(user, password);
        return new HttpEntity<>("parameters",
                                headers);
    }

    public HttpEntity entityWithoutAuthentication() {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity<>("parameters", headers);
    }

    public HttpHeaders authorizationHeaders() {
        return this.authorizationHeaders(keycloakTestUser, keycloakTestPassword);
    }

    public HttpHeaders authorizationHeaders(String user, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER,
                    getTokenString(user, password));
        return headers;
    }

    public void setKeycloakTestUser(String keycloakTestUser) {
        this.keycloakTestUser = keycloakTestUser;
    }

    public String getKeycloakTestUser() {
        return keycloakTestUser;
    }

}
