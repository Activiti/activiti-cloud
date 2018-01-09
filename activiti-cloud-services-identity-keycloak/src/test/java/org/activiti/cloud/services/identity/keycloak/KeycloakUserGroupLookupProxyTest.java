package org.activiti.cloud.services.identity.keycloak;

import org.activiti.engine.UserGroupLookupProxy;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;


import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.assertj.core.api.Assertions.assertThat;

public class KeycloakUserGroupLookupProxyTest {

    private UserGroupLookupProxy userGroupLookupProxy;

    @Mock
    private KeycloakLookupService keycloakLookupService;



    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userGroupLookupProxy = new KeycloakUserGroupLookupProxy(keycloakLookupService);
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
        assertThat(userGroupLookupProxy.getGroupsForCandidateUser("bob")).contains("testgroup");
    }


}