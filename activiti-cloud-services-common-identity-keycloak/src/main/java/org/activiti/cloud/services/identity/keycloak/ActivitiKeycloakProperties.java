package org.activiti.cloud.services.identity.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "activiti.keycloak")
public class ActivitiKeycloakProperties {

    private String adminClientApp;

    private String clientUser;

    private String clientPassword;

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
}
