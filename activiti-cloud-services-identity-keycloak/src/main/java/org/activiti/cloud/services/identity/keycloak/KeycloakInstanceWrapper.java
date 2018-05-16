package org.activiti.cloud.services.identity.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class KeycloakInstanceWrapper {

    @Autowired
    private ActivitiKeycloakProperties activitiKeycloakProperties;

    @Autowired
    private KeycloakProperties keycloakProperties;

    private Keycloak getKeycloakInstance() {
        return Keycloak.getInstance(keycloakProperties.getAuthServerUrl(),
                keycloakProperties.getRealm(),
                activitiKeycloakProperties.getClientUser(),
                activitiKeycloakProperties.getClientPassword(),
                activitiKeycloakProperties.getAdminClientApp());
    }

    public RealmResource getRealm(){
        return getKeycloakInstance().realms().realm(keycloakProperties.getRealm());
    }

}
