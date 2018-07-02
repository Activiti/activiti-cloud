package org.activiti.cloud.services.identity.keycloak;

import java.util.ArrayList;
import java.util.List;

import org.activiti.runtime.api.identity.IdentityLookup;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KeycloakIdentityLookupTest {

    private IdentityLookup identityLookup;

    @Mock
    private KeycloakLookupService keycloakLookupService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        identityLookup = new KeycloakIdentityLookup(keycloakLookupService);
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("id");
        List<GroupRepresentation> groupRepresentations = new ArrayList<>();
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName("testgroup");
        groupRepresentations.add(groupRepresentation);

        when(keycloakLookupService.getUser(anyString())).thenReturn(userRepresentation);
        when(keycloakLookupService.getGroupsForUser(anyString())).thenReturn(groupRepresentations);
    }

    @Test
    public void testGetGroups() {
        assertThat(identityLookup.getGroupsForCandidateUser("bob")).contains("testgroup");
    }
}