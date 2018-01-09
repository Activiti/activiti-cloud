package org.activiti.cloud.services.identity.keycloak;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeycloakLookupService {
    @Autowired
    private KeycloakInstanceWrapper keycloakInstanceWrapper;


    public List<GroupRepresentation> getGroupsForUser(String userId){
        return keycloakInstanceWrapper.getRealm().users().get(userId).groups();
    }

    public List<RoleRepresentation> getRolesForUser(String userId){
        return keycloakInstanceWrapper.getRealm().users().get(userId).roles().realmLevel().listEffective();
    }

    public UserRepresentation getUser(String userIdentifier) throws UnsupportedOperationException {
        List<UserRepresentation> users = keycloakInstanceWrapper.getRealm().users().search(userIdentifier,
                0,
                2);

        if (users.size() > 1) {
            throw new UnsupportedOperationException("User id " + userIdentifier + " is not unique");
        }
        if (users.size() == 0) {
            throw new UnsupportedOperationException("User id " + userIdentifier + " not found");
        }
        return users.get(0);
    }
}
