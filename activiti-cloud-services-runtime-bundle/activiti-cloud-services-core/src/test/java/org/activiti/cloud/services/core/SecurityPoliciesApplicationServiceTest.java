package org.activiti.cloud.services.core;

import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        map.put("rb1",new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),any(),any(SecurityPolicy.class))).thenReturn(map);

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
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        map.put("rb1",new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.READ)).thenReturn(map);

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
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        map.put("rb1",new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.READ)).thenReturn(map);

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        securityPoliciesApplicationService.restrictProcessInstQuery(query,SecurityPolicy.READ);

        verify(query,times(1)).processDefinitionKeys(anySet());
    }

    @Test
    public void shouldRestrictQueryWhenPoliciesButNotForUser(){

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");
        when(userRoleLookupProxy.isAdmin("intruder")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("intruder")).thenReturn(null);
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        when(securityPoliciesService.getProcessDefinitionKeys("intruder",null,SecurityPolicy.READ)).thenReturn(map);

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(query.processDefinitionId(any())).thenReturn(query);
        securityPoliciesApplicationService.restrictProcessInstQuery(query,SecurityPolicy.READ);

        verify(query,times(2)).processDefinitionId(any());
    }
}
