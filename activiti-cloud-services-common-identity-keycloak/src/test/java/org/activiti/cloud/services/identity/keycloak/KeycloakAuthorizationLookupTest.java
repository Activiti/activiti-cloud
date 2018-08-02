package org.activiti.cloud.services.identity.keycloak;

import java.util.ArrayList;
import java.util.List;

import org.activiti.runtime.api.identity.UserGroupManager;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class KeycloakAuthorizationLookupTest {

    private UserGroupManager userGroupManager;

    @Mock
    private KeycloakLookupService keycloakLookupService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userGroupManager = new KeycloakUserGroupManager(keycloakLookupService);


        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("id");

        when(keycloakLookupService.getUser(anyString())).thenReturn(userRepresentation);
    }

    @Test
    public void testGetRoles() {

        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("testrole");
        roleRepresentations.add(roleRepresentation);
        when(keycloakLookupService.getRolesForUser(anyString())).thenReturn(roleRepresentations);

        assertThat(userGroupManager.getUserRoles("bob")).contains("testrole");
        assertThat(userGroupManager.getUserRoles("bob")).doesNotContain("admin");

    }

    @Test
    public void testAdmin() {

        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("admin");
        roleRepresentations.add(roleRepresentation);
        when(keycloakLookupService.getRolesForUser(anyString())).thenReturn(roleRepresentations);

        assertThat(userGroupManager.getUserRoles("bob")).contains("admin");
    }
}