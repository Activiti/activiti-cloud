package org.activiti.cloud.services.identity.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix="keycloak")
@Component
public class KeycloakProperties {

    private String authServerUrl;
    
    private String realm;

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
