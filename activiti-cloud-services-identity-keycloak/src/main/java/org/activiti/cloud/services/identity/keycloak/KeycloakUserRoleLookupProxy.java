package org.activiti.cloud.services.identity.keycloak;

import org.activiti.engine.UserRoleLookupProxy;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeycloakUserRoleLookupProxy implements UserRoleLookupProxy {

    @Value("${admin-role-name:admin}")
    private String adminRoleName;

    private KeycloakInstanceWrapper keycloakInstanceWrapper;

    @Autowired
    public KeycloakUserRoleLookupProxy(KeycloakInstanceWrapper keycloakInstanceWrapper){
        this.keycloakInstanceWrapper = keycloakInstanceWrapper;
    }

    public List<String> getRolesForUser(String userName) {

        List<UserRepresentation> users = keycloakInstanceWrapper.getUser(userName);
        if (users.size() > 1) {
            throw new UnsupportedOperationException("User id " + userName + " is not unique");
        }

        UserRepresentation user = users.get(0);

        List<RoleRepresentation> roleRepresentations = keycloakInstanceWrapper.getRolesForUser(user.getId());

        List<String> roles = null;
        if (roleRepresentations != null && roleRepresentations.size() > 0) {
            roles = new ArrayList<String>();
            for (RoleRepresentation roleRepresentation : roleRepresentations) {
                roles.add(roleRepresentation.getName());
            }
        }

        return roles;
    }

    public boolean isAdmin(String userId){
        List<String> roles = getRolesForUser(userId);
        return (roles != null && roles.contains(adminRoleName));
    }

    public void setAdminRoleName(String adminRoleName) {
        this.adminRoleName = adminRoleName;
    }

}
