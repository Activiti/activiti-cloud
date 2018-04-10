package org.activiti.cloud.services.security;

import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseSecurityPoliciesApplicationServiceTest {

    @InjectMocks
    @Spy
    private BaseSecurityPoliciesApplicationService securityPoliciesApplicationService;

    @Mock
    private UserGroupLookupProxy userGroupLookupProxy;

    @Mock
    private UserRoleLookupProxy userRoleLookupProxy;

    @Mock
    private SecurityPoliciesService securityPoliciesService;

    @Mock
    private BaseAuthenticationWrapper authenticationWrapper;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldHavePermissionWhenNoPoliciesDefined(){
        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        assertThat(securityPoliciesApplicationService.canRead("key","rb1")).isTrue();

    }

    @Test
    public void shouldHavePermissionWhenDefIsInPolicy(){
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        map.put("rb1",new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups, SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups, SecurityPolicy.READ)).thenReturn(map);

        assertThat(securityPoliciesApplicationService.canRead("key","rb1")).isTrue();
        assertThat(securityPoliciesApplicationService.canWrite("key","rb1")).isTrue();
    }

    @Test
    public void shouldNotHavePermissionWhenDefIsNotInPolicy(){
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        map.put("rb1",new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups, SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups, SecurityPolicy.READ)).thenReturn(map);

        assertThat(securityPoliciesApplicationService.canRead("otherKey","rb1")).isFalse();
        assertThat(securityPoliciesApplicationService.canWrite("key","rb2")).isFalse();
    }


    @Test
    public void shouldHavePermissionWhenAdmin(){

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("admin");
        when(userRoleLookupProxy.isAdmin("admin")).thenReturn(true);

        assertThat(securityPoliciesApplicationService.canRead("key","rb1")).isTrue();
    }

    @Test
    public void shouldBeNoPoliciesOrNoUserWhenNoPolicies(){
        when(securityPoliciesService.policiesDefined()).thenReturn(false);
        assertThat(securityPoliciesApplicationService.noSecurityPoliciesOrNoUser()).isTrue();
    }

    @Test
    public void shouldBeNoPoliciesOrNoUserWhenNoUser(){
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn(null);
        assertThat(securityPoliciesApplicationService.noSecurityPoliciesOrNoUser()).isTrue();
    }
}
