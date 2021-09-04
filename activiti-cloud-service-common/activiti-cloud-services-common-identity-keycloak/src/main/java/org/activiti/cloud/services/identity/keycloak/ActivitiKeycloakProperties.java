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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "activiti.keycloak")
public class ActivitiKeycloakProperties {

    public static enum GrantType {
        password, client_credentials
    }

    private String adminClientApp;

    private String clientUser;

    private String clientPassword;

    private String clientId;

    private String clientSecret;

    private GrantType grantType = GrantType.password;

    public String getAdminClientApp() {
        return adminClientApp;
    }

    public String getClientUser() {
        return clientUser;
    }

    public String getClientPassword() {
        return clientPassword;
    }

    public void setAdminClientApp(String adminClientApp) {
        this.adminClientApp = adminClientApp;
    }

    public void setClientUser(String clientUser) {
        this.clientUser = clientUser;
    }

    public void setClientPassword(String clientPassword) {
        this.clientPassword = clientPassword;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public void setGrantType(GrantType grantType) {
        this.grantType = grantType;
    }

}
