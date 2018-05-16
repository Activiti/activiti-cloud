package org.activiti.cloud.services.test.identity.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.activiti.cloud.services.identity.keycloak.KeycloakInstanceWrapper;
import org.activiti.cloud.services.identity.keycloak.KeycloakLookupService;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class KeycloakLookupServiceTest {

    @InjectMocks
    private KeycloakLookupService keycloakLookupService;

    @Mock
    private KeycloakInstanceWrapper keycloakInstanceWrapper;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private GroupRepresentation groupRepresentation;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetGroupsForUser() {
        when(keycloakInstanceWrapper.getRealm()).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any())).thenReturn(userResource);
        when(userResource.groups()).thenReturn(Arrays.asList(groupRepresentation));
        assertThat(keycloakLookupService.getGroupsForUser("bob")).contains(groupRepresentation);
    }

    @Test
    public void testGetRolesForUser() {
        when(keycloakInstanceWrapper.getRealm()).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listEffective()).thenReturn(new ArrayList<>());
        assertThat(keycloakLookupService.getRolesForUser("bob")).isNotNull();
    }

    @Test
    public void testGetUser() {
        when(keycloakInstanceWrapper.getRealm()).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any())).thenReturn(userResource);
        when(usersResource.search("bob",
                                  0,
                                  2)).thenReturn(Arrays.asList(new UserRepresentation()));
        assertThat(keycloakLookupService.getUser("bob")).isNotNull();
    }

    @Test
    public void testMustBeUniqueUser() {

        List<UserRepresentation> users = new ArrayList<>();
        UserRepresentation userRepresentation1 = new UserRepresentation();
        userRepresentation1.setId("id1");
        users.add(userRepresentation1);

        UserRepresentation userRepresentation2 = new UserRepresentation();
        userRepresentation2.setId("id2");
        users.add(userRepresentation2);

        when(keycloakInstanceWrapper.getRealm()).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("fred",
                                  0,
                                  2)).thenReturn(users);

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> keycloakLookupService.getUser("fred"));
    }

    @Test
    public void testGetNoUser() {
        when(keycloakInstanceWrapper.getRealm()).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any())).thenReturn(userResource);
        when(usersResource.search("bob",
                                  0,
                                  2)).thenReturn(new ArrayList<>());
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> keycloakLookupService.getUser("bob"));
    }
}
