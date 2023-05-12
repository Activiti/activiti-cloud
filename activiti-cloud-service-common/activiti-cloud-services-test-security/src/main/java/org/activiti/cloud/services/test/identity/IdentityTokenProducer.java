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
package org.activiti.cloud.services.test.identity;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public interface IdentityTokenProducer {
    public static final String AUTHORIZATION_HEADER = "Authorization";

    String getTokenString();

    IdentityTokenProducer withDefaultValues();

    IdentityTokenProducer withResource(String resource);
    IdentityTokenProducer withTestPassword(String password);
    IdentityTokenProducer withTestUser(String user);

    String getAccessTokenString();

    HttpEntity entityWithAuthorizationHeader(String user, String password);

    HttpEntity entityWithoutAuthentication();

    HttpEntity entityWithAuthorizationHeader();

    String getTestUser();

    HttpHeaders authorizationHeaders();
}
