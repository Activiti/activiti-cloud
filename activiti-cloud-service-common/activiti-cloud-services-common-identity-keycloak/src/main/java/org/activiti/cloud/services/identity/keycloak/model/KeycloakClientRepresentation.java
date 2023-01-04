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
import org.activiti.cloud.identity.IdentityClientRepresentation;

public class KeycloakClientRepresentation implements IdentityClientRepresentation {

    private String id;

    private String clientId;
    private String name;
    private Boolean enabled;
    private Boolean standardFlowEnabled;
    private Boolean directAccessGrantsEnabled;
    private Boolean authorizationServicesEnabled;
    private String secret;
    private List<String> redirectUris;
    private List<String> webOrigins;
    private Boolean publicClient;
    private Boolean implicitFlowEnabled;
    private Boolean serviceAccountsEnabled;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public Boolean getDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public Boolean getAuthorizationServicesEnabled() {
        return authorizationServicesEnabled;
    }

    public void setAuthorizationServicesEnabled(Boolean authorizationServicesEnabled) {
        this.authorizationServicesEnabled = authorizationServicesEnabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Boolean getPublicClient() {
        return publicClient;
    }

    public void setPublicClient(Boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Boolean getImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public Boolean getServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static class Builder {

        private String clientId;
        private String name;
        private Boolean enabled;
        private Boolean standardFlowEnabled;
        private Boolean directAccessGrantsEnabled;
        private Boolean authorizationServicesEnabled;
        private String secret;
        private List<String> redirectUris;
        private List<String> webOrigins;
        private Boolean publicClient;
        private Boolean implicitFlowEnabled;
        private Boolean serviceAccountsEnabled;


        public static Builder newKeycloakClientRepresentationBuilder() {
            return new Builder();
        }

        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withClientName(String clientName) {
            this.name = clientName;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder standardFlowEnabled(Boolean standardFlowEnabled) {
            this.standardFlowEnabled = standardFlowEnabled;
            return this;
        }

        public Builder directAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
            this.directAccessGrantsEnabled = directAccessGrantsEnabled;
            return this;
        }

        public Builder authorizationServicesEnabled(Boolean authorizationServicesEnabled) {
            this.authorizationServicesEnabled = authorizationServicesEnabled;
            return this;
        }

        public Builder withSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder withRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris;
            return this;
        }

        public Builder withWebOrigins(List<String> webOrigins) {
            this.webOrigins = webOrigins;
            return this;
        }

        public Builder publicClient(Boolean publicClient) {
            this.publicClient = publicClient;
            return this;
        }

        public Builder implicitFlowEnabled(Boolean implicitFlowEnabled) {
            this.implicitFlowEnabled = implicitFlowEnabled;
            return this;
        }

        public Builder serviceAccountsEnabled(Boolean serviceAccountsEnabled) {
            this.serviceAccountsEnabled = serviceAccountsEnabled;
            return this;
        }

        public KeycloakClientRepresentation build() {
            KeycloakClientRepresentation keycloakClientRepresentation = new KeycloakClientRepresentation();
            keycloakClientRepresentation.setClientId(clientId);
            keycloakClientRepresentation.setName(name);
            keycloakClientRepresentation.setEnabled(enabled);
            keycloakClientRepresentation.setStandardFlowEnabled(standardFlowEnabled);
            keycloakClientRepresentation.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
            keycloakClientRepresentation.setAuthorizationServicesEnabled(authorizationServicesEnabled);
            keycloakClientRepresentation.setSecret(secret);
            keycloakClientRepresentation.setRedirectUris(redirectUris);
            keycloakClientRepresentation.setWebOrigins(webOrigins);
            keycloakClientRepresentation.setPublicClient(publicClient);
            keycloakClientRepresentation.setImplicitFlowEnabled(implicitFlowEnabled);
            keycloakClientRepresentation.setServiceAccountsEnabled(serviceAccountsEnabled);
            return keycloakClientRepresentation;
        }
    }
}
