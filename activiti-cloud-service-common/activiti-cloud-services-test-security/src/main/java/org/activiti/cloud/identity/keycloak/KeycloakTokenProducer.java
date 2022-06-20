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
package org.activiti.cloud.identity.keycloak;

import org.activiti.cloud.identity.IdentityTokenProducer;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public class KeycloakTokenProducer implements IdentityTokenProducer {

    private final String authServerUrl;

    private final String realm;

    private String resource;

    private String testUser;

    private String testPassword;

    public KeycloakTokenProducer(String authServerUrl, String realm) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
    }

    @Override
    public String getTokenString() {
        return "Bearer " + getAccessTokenString();
    }

    @Override
    public String getAccessTokenString() {
        AccessTokenResponse token = getAccessTokenResponse(testUser, testPassword);
        return token.getToken();
    }

    @Override
    public IdentityTokenProducer withTestUser(String keycloakTestUser) {
        this.testUser = keycloakTestUser;
        return this;
    }

    @Override
    public IdentityTokenProducer withTestPassword(String testPassword) {
        this.testPassword = testPassword;
        return this;
    }

    public IdentityTokenProducer withResource(String resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public HttpEntity entityWithAuthorizationHeader(String user, String password) {
        HttpHeaders headers = authorizationHeaders(user, password);
        return new HttpEntity<>("parameters", headers);
    }

    @Override
    public HttpEntity entityWithoutAuthentication() {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity<>("parameters", headers);
    }

    @Override
    public HttpEntity entityWithAuthorizationHeader() {
        HttpHeaders headers = authorizationHeaders();
        return new HttpEntity<>("parameters", headers);
    }

    @Override
    public String getTestUser() {
        return testUser;
    }

    @Override
    public HttpHeaders authorizationHeaders() {
        return this.authorizationHeaders(testUser, testPassword);
    }

    private String getTokenString(String user, String password) {
        AccessTokenResponse token = getAccessTokenResponse(user, password);
        return "Bearer " + token.getToken();
    }

    private HttpHeaders authorizationHeaders(String user, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER, getTokenString(user, password));
        return headers;
    }

    private AccessTokenResponse getAccessTokenResponse(String user, String password) {
        return Keycloak.getInstance(authServerUrl,
                                    realm,
                                    user,
                                    password,
                                    resource).tokenManager().getAccessToken();
    }

}
