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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.activiti.cloud.identity.IdentityClientRepresentation;

import java.util.List;

public class KeycloakCredentialRepresentation  {

    private String type;
    private String value;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static class Builder {

        private String type;
        private String value;

        public static Builder newKeycloakCredentialRepresentationBuilder() {
            return new Builder();
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withValue(String value) {
            this.value = value;
            return this;
        }


        public KeycloakCredentialRepresentation build() {
            KeycloakCredentialRepresentation keycloakClientRepresentation = new KeycloakCredentialRepresentation();
            keycloakClientRepresentation.setType(type);
            keycloakClientRepresentation.setValue(value);
            return keycloakClientRepresentation;
        }
    }
}
