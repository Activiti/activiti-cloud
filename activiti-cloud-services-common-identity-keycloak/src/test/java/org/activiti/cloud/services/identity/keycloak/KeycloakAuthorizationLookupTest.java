package org.activiti.cloud.services.identity.keycloak;

import java.util.ArrayList;
import java.util.List;

import org.activiti.runtime.api.auth.AuthorizationLookup;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KeycloakAuthorizationLookupTest {

    private AuthorizationLookup authorizationLookup;

    @Mock
    private KeycloakLookupService keycloakLookupService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authorizationLookup = new KeycloakAuthorizationLookup(keycloakLookupService);
        ((KeycloakAuthorizationLookup) authorizationLookup).setAdminRoleName("admin");

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

        assertThat(authorizationLookup.getRolesForUser("bob")).contains("testrole");
        assertThat(authorizationLookup.isAdmin("bob")).isFalse();
    }

    @Test
    public void testAdmin() {

        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName("admin");
        roleRepresentations.add(roleRepresentation);
        when(keycloakLookupService.getRolesForUser(anyString())).thenReturn(roleRepresentations);

        assertThat(authorizationLookup.isAdmin("bob")).isTrue();
    }
}