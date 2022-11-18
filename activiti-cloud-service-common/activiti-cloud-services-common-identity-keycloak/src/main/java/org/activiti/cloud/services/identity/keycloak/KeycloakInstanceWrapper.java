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
package org.activiti.cloud.services.identity.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;

public class KeycloakInstanceWrapper {

    @Autowired
    private ActivitiKeycloakProperties activitiKeycloakProperties;

    @Autowired
    private KeycloakProperties keycloakProperties;

    private Keycloak getKeycloakInstance() {
        KeycloakBuilder client = KeycloakBuilder
            .builder()
            .serverUrl(keycloakProperties.getAuthServerUrl())
            .realm(keycloakProperties.getRealm())
            .clientId(activitiKeycloakProperties.getClientId())
            .clientSecret(activitiKeycloakProperties.getClientSecret())
            .grantType(activitiKeycloakProperties.getGrantType().name());
        return client.build();
    }

    public RealmResource getRealm() {
        return getKeycloakInstance().realms().realm(keycloakProperties.getRealm());
    }
}
