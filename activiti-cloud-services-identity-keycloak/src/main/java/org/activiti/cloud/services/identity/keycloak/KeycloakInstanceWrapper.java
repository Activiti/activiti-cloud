package org.activiti.cloud.services.identity.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeycloakInstanceWrapper {

    @Value("${keycloak.auth-server-url}")
    private String authServer;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloakadminclientapp}")
    private String keycloakadminclientapp;

    @Value("${keycloakclientuser}")
    private String clientUser;

    @Value("${keycloakclientpassword}")
    private String clientPassword;

    private Keycloak getKeycloakInstance() {
        return Keycloak.getInstance(authServer,
                realm,
                clientUser,
                clientPassword,
                keycloakadminclientapp);
    }

    protected RealmResource getRealm(){
        return getKeycloakInstance().realms().realm(realm);
    }

    public List<GroupRepresentation> getGroupsForUser(String userId){
        return getRealm().users().get(userId).groups();
    }

    public List<RoleRepresentation> getRolesForUser(String userId){
        return getRealm().users().get(userId).roles().realmLevel().listEffective();
    }

    public List<UserRepresentation> getUser(String userIdentifier){
        List<UserRepresentation> users = getRealm().users().search(userIdentifier,
                0,
                10);
        return users;
    }
}
