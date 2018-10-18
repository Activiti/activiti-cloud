/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.stereotype.Component;

@Component
public class KeycloakTokenProducer implements ClientHttpRequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private KeycloakProperties keycloakProperties;

    @Value("${keycloak.resource:}")
    protected String resource;

    @Value("${activiti.keycloak.test-user:}")
    protected String keycloakTestUser;

    @Value("${activiti.keycloak.test-password:}")
    protected String keycloakTestPassword;

    public KeycloakTokenProducer(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest,
                                        byte[] bytes,
                                        ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        httpRequest.getHeaders().set(AUTHORIZATION_HEADER,
                                     getTokenString());
        return clientHttpRequestExecution.execute(httpRequest,
                                                  bytes);
    }

    private AccessTokenResponse getAccessTokenResponse() {
        return Keycloak.getInstance(keycloakProperties.getAuthServerUrl(),
                                    keycloakProperties.getRealm(),
                                    keycloakTestUser,
                                    keycloakTestPassword,
                                    resource).tokenManager().getAccessToken();
    }

    private String getTokenString() {
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

    public void setKeycloakTestUser(String keycloakTestUser) {
        this.keycloakTestUser = keycloakTestUser;
    }

    public String getKeycloakTestUser() {
        return keycloakTestUser;
    }
}