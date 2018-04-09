package org.activiti.cloud.services.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Mock
    private SecurityPoliciesProcessDefinitionRestrictionApplier processDefinitionRestrictionApplier;

    @Mock
    private SecurityPoliciesProcessInstanceRestrictionApplier processInstanceRestrictionApplier;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

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
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailableForRB(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));
        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1", Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),any(),any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getName()).thenReturn("rb1");

        securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).restrictToKeys(any(), anySet());

    }


    @Test
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailableAndHaveNoRBName(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));
        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1", Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),any(),any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getName()).thenReturn(null);

        securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).restrictToKeys(any(), anySet());

    }

    @Test
    public void shouldOnlyUsePoliciesForThisRB(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));
        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1", Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),any(),any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getName()).thenReturn("rb2");

        securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).denyAll(any());
    }

    @Test
    public void shouldNotRestrictQueryWhenPolicyIsWildcard(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));
        when(securityPoliciesService.getWildcard()).thenReturn("*");

        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1", Collections.singleton(securityPoliciesService.getWildcard()));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),any(),any(SecurityPolicy.class))).thenReturn(map);

        securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ);

        assertThat(securityPoliciesApplicationService.restrictProcessDefQuery(query, SecurityPolicy.READ)).isEqualTo(query);

    }

    @Test
    public void shouldHavePermissionWhenDefIsInPolicy(){
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1", Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.READ)).thenReturn(map);

        assertThat(securityPoliciesApplicationService.canWrite("key")).isTrue();
        assertThat(securityPoliciesApplicationService.canRead("key")).isTrue();
    }

    @Test
    public void shouldHavePermissionWhenDefIsIsCoveredByWildcard(){
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);
        when(securityPoliciesService.getWildcard()).thenReturn("*");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1",Collections.singleton(securityPoliciesService.getWildcard()));
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
        Map<String,Set<String>> map = new HashMap<>();
        map.put("rb1",Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",groups,SecurityPolicy.READ)).thenReturn(map);

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        securityPoliciesApplicationService.restrictProcessInstQuery(query,SecurityPolicy.READ);

        verify(processInstanceRestrictionApplier).restrictToKeys(any(), anySet());
    }

    @Test
    public void shouldRestrictQueryWhenPoliciesButNotForUser(){

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");
        when(userRoleLookupProxy.isAdmin("intruder")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("intruder")).thenReturn(null);
        Map<String,Set<String>> map = new HashMap<>();
        when(securityPoliciesService.getProcessDefinitionKeys("intruder",null,SecurityPolicy.READ)).thenReturn(map);

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(query.processDefinitionId(any())).thenReturn(query);
        securityPoliciesApplicationService.restrictProcessInstQuery(query,SecurityPolicy.READ);

        verify(processInstanceRestrictionApplier).denyAll(any());
    }


    @Test
    public void shouldRestrictProcDefQueryWhenPoliciesButNotForUser(){

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");
        when(userRoleLookupProxy.isAdmin("intruder")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("intruder")).thenReturn(null);
        Map<String,Set<String>> map = new HashMap<>();
        when(securityPoliciesService.getProcessDefinitionKeys("intruder",null,SecurityPolicy.READ)).thenReturn(map);

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(query.processDefinitionId(any())).thenReturn(query);
        securityPoliciesApplicationService.restrictProcessDefQuery(query,SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).denyAll(any());
    }
}
