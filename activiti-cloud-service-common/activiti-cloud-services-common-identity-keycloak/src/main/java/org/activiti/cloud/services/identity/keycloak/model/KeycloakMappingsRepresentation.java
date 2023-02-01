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
package org.activiti.cloud.services.identity.keycloak.model;

import java.util.List;
import java.util.Map;

public class KeycloakMappingsRepresentation {

    protected List<KeycloakRoleMapping> realmMappings;
    protected Map<String, KeycloakClientMappingsRepresentation> clientMappings;

    public List<KeycloakRoleMapping> getRealmMappings() {
        return realmMappings;
    }

    public void setRealmMappings(
        List<KeycloakRoleMapping> realmMappings) {
        this.realmMappings = realmMappings;
    }

    public Map<String, KeycloakClientMappingsRepresentation> getClientMappings() {
        return clientMappings;
    }

    public void setClientMappings(
        Map<String, KeycloakClientMappingsRepresentation> clientMappings) {
        this.clientMappings = clientMappings;
    }
}
