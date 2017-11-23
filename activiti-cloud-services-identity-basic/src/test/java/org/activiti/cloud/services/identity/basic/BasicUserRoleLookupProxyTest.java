package org.activiti.cloud.services.identity.basic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BasicUserRoleLookupProxyTest {

    @InjectMocks
    private BasicUserRoleLookupProxy userRoleLookupProxy;

    @Mock
    private BasicUserGroupLookupProxy userGroupLookupProxy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userRoleLookupProxy.setAdminRoleName("admin");
    }

    @Test
    public void testGetRoles() {

        List<String> roles = new ArrayList<>();
        roles.add("role");

        when(userGroupLookupProxy.getGroupsForCandidateUser("test"))
                .thenReturn(roles);

        assertThat(userRoleLookupProxy.getRolesForUser("test")).contains("role");
        assertThat(userRoleLookupProxy.isAdmin("test")).isFalse();
    }

    @Test
    public void testAdminRole()  {

        List<String> roles = new ArrayList<>();
        roles.add("admin");

        when(userGroupLookupProxy.getGroupsForCandidateUser("admin"))
                .thenReturn(roles);

        assertThat(userRoleLookupProxy.isAdmin("admin")).isTrue();
    }

}
