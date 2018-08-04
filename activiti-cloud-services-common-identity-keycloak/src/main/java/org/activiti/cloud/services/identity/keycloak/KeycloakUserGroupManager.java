/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.identity.keycloak;

import org.activiti.runtime.api.identity.UserGroupManager;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("userGroupManager")
public class KeycloakUserGroupManager implements UserGroupManager {


    private KeycloakInstanceWrapper keycloakInstanceWrapper;

    @Autowired
    public KeycloakUserGroupManager(KeycloakInstanceWrapper keycloakInstanceWrapper) {
        this.keycloakInstanceWrapper = keycloakInstanceWrapper;
    }

    @Override
    public List<String> getUserGroups(String username) {
        // Verify that the user exist
        UserRepresentation user = getUser(username);

        List<GroupRepresentation> groupRepresentations = keycloakInstanceWrapper.getRealm().users().get(user.getId()).groups();

        List<String> groups = null;
        if (groupRepresentations != null && groupRepresentations.size() > 0) {
            groups = new ArrayList<String>();
            for (GroupRepresentation groupRepresentation : groupRepresentations) {
                groups.add(groupRepresentation.getName());
            }
        }

        return groups;
    }

    @Override
    public List<String> getUserRoles(String username) {
        // Verify that the user exist
        UserRepresentation user = getUser(username);

        List<RoleRepresentation> rolesRepresentations = keycloakInstanceWrapper.getRealm().users().get(user.getId()).roles().realmLevel().listEffective();

        List<String> roles = null;
        if (rolesRepresentations != null && rolesRepresentations.size() > 0) {
            roles = new ArrayList<String>();
            for (RoleRepresentation roleRepresentation : rolesRepresentations) {
                roles.add(roleRepresentation.getName());
            }
        }

        return roles;
    }

    private UserRepresentation getUser(String username) {
        List<UserRepresentation> users = keycloakInstanceWrapper.getRealm().users().search(username,
                0,
                2);

        if (users.size() > 1) {
            throw new UnsupportedOperationException("User id " + username + " is not unique");
        }
        if (users.size() == 0) {
            throw new UnsupportedOperationException("User id " + username + " not found");
        }
        return users.get(0);
    }

}
