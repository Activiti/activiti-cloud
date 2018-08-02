package org.activiti.cloud.services.identity.keycloak;

import org.activiti.runtime.api.identity.UserGroupManager;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

public class KeycloakUserGroupManagerTest {

    private UserGroupManager userGroupManager;

    @Mock
    private KeycloakLookupService keycloakLookupService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userGroupManager = new KeycloakUserGroupManager(keycloakLookupService);
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
        assertThat(userGroupManager.getUserGroups("bob")).contains("testgroup");
    }
}