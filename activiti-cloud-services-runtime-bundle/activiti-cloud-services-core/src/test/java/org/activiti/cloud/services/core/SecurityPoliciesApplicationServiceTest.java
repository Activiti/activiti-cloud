package org.activiti.cloud.services.core;

import org.activiti.cloud.services.SecurityPolicy;
import org.activiti.cloud.services.SecurityPoliciesService;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityPoliciesApplicationServiceTest {

    @InjectMocks
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @Mock
    private UserGroupLookupProxy userGroupLookupProxy;

    @Mock
    private UserRoleLookupProxy userRoleLookupProxy;

    @Mock
    private SecurityPoliciesService securityPoliciesService;

    @Mock
    private AuthenticationWrapper authenticationWrapper;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldNotModifyQueryWhenNoPoliciesDefined(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(false);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        assertThat(securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ)).isEqualTo(query);
    }

    @Test
    public void shouldNotModifyQueryWhenNoUser(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn(null);

        assertThat(securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ)).isEqualTo(query);
    }

    @Test
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailable(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));

        securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ);

        verify(query).processDefinitionKeys(anySet());

    }

    @Test
    public void shouldHavePermissionWhenDefIsInPolicy(){
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.WRITE)).thenReturn(new HashSet<>(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.READ)).thenReturn(new HashSet<>(Arrays.asList("key")));

        assertThat(securityPoliciesApplicationService.canWrite("key")).isTrue();
        assertThat(securityPoliciesApplicationService.canRead("key")).isTrue();
    }

    @Test
    public void shouldHavePermissionWhenAdmin(){

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("admin");
        when(userRoleLookupProxy.isAdmin("admin")).thenReturn(true);

        assertThat(securityPoliciesApplicationService.canWrite("key")).isTrue();
        assertThat(securityPoliciesApplicationService.canRead("key")).isTrue();
    }

    @Test
    public void shouldRestrictQueryWhenKeysFromPolicy(){
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.READ)).thenReturn(new HashSet<>(Arrays.asList("key")));

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        securityPoliciesApplicationService.restrictProcessInstQuery(query,SecurityPolicy.READ);

        verify(query).processDefinitionKeys(anySet());
    }
}
