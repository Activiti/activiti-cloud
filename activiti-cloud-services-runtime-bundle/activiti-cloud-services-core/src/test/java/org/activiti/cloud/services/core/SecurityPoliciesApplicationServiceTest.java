package org.activiti.cloud.services.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.runtime.api.auth.AuthorizationLookup;
import org.activiti.runtime.api.identity.IdentityLookup;
import org.activiti.runtime.api.model.payloads.GetProcessDefinitionsPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityPoliciesApplicationServiceTest {

    @InjectMocks
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @Mock
    private IdentityLookup identityLookup;

    @Mock
    private AuthorizationLookup authorizationLookup;

    @Mock
    private SecurityPoliciesService securityPoliciesService;

    @Mock
    private SpringSecurityAuthenticationWrapper authenticationWrapper;

    @Mock
    private SecurityPoliciesProcessDefinitionRestrictionApplier processDefinitionRestrictionApplier;

    @Mock
    private SecurityPoliciesProcessInstanceRestrictionApplier processInstanceRestrictionApplier;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldAllowAllWhenNoPoliciesDefined() {
        //given
        when(securityPoliciesService.policiesDefined()).thenReturn(false);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        GetProcessDefinitionsPayload allowAllFilter = mock(GetProcessDefinitionsPayload.class);
        when(processDefinitionRestrictionApplier.allowAll()).thenReturn(allowAllFilter);

        //when
        GetProcessDefinitionsPayload actualFilter = securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        //then
        assertThat(actualFilter).isEqualTo(allowAllFilter);
    }

    @Test
    public void shouldAllowAllWhenNoUser() {
        //given
        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn(null);

        GetProcessDefinitionsPayload allowAllFilter = mock(GetProcessDefinitionsPayload.class);
        when(processDefinitionRestrictionApplier.allowAll()).thenReturn(allowAllFilter);

        //when
        GetProcessDefinitionsPayload actualFilter = securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        //then
        assertThat(actualFilter).isEqualTo(allowAllFilter);
    }

    @Test
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailableForRB() {
        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        Set<String> keys = Collections.singleton("key");
        GetProcessDefinitionsPayload filter = mock(GetProcessDefinitionsPayload.class);
        when(processDefinitionRestrictionApplier.restrictToKeys(keys)).thenReturn(filter);

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(Collections.singletonList("hr"));
        Map<String, Set<String>> map = Collections.singletonMap("rb1",
                                                                keys);
        when(securityPoliciesService.getProcessDefinitionKeys(any(),
                                                              any(),
                                                              any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getServiceName()).thenReturn("rb1");

        //when
        GetProcessDefinitionsPayload actualFilter = securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        //then
        assertThat(actualFilter).isEqualTo(filter);
    }

    @Test
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailableForRBFullName() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(Collections.singletonList("hr"));
        Map<String, Set<String>> map = new HashMap<>();
        map.put("app1-rb1",
                Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),
                                                              any(),
                                                              any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getServiceFullName()).thenReturn("app1-rb1");

        //when
        securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).restrictToKeys(anySet());
    }

    @Test
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailableAndHaveNoRBName() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(Collections.singletonList("hr"));
        Map<String, Set<String>> map = new HashMap<>();
        map.put("rb1",
                Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),
                                                              any(),
                                                              any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getServiceName()).thenReturn(null);

        securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).restrictToKeys(anySet());
    }

    @Test
    public void shouldOnlyUsePoliciesForThisRB() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(Collections.singletonList("hr"));
        Map<String, Set<String>> map = new HashMap<>();
        map.put("rb1",
                Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),
                                                              any(),
                                                              any(SecurityPolicy.class))).thenReturn(map);
        when(runtimeBundleProperties.getServiceName()).thenReturn("rb2");

        securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).denyAll();
    }

    @Test
    public void shouldNotRestrictQueryWhenPolicyIsWildcard() {
        //given
        GetProcessDefinitionsPayload filter = mock(GetProcessDefinitionsPayload.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(Collections.singletonList("hr"));
        when(securityPoliciesService.getWildcard()).thenReturn("*");

        Map<String, Set<String>> map = new HashMap<>();
        map.put("rb1",
                Collections.singleton(securityPoliciesService.getWildcard()));
        when(securityPoliciesService.getProcessDefinitionKeys(any(),
                                                              any(),
                                                              any(SecurityPolicy.class))).thenReturn(map);
        when(processDefinitionRestrictionApplier.allowAll()).thenReturn(filter);

        //when
        GetProcessDefinitionsPayload actualFilter = securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        //then
        assertThat(actualFilter).isEqualTo(filter);
    }

    @Test
    public void shouldHavePermissionWhenDefIsInPolicy() {
        List<String> groups = Collections.singletonList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(authorizationLookup.isAdmin("bob")).thenReturn(false);

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String, Set<String>> map = new HashMap<>();
        map.put("rb1",
                Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.READ)).thenReturn(map);
        when(runtimeBundleProperties.getServiceName()).thenReturn("rb1");

        assertThat(securityPoliciesApplicationService.canWrite("key")).isTrue();
        assertThat(securityPoliciesApplicationService.canRead("key")).isTrue();
    }

    @Test
    public void shouldHavePermissionWhenDefIsIsCoveredByWildcard() {
        List<String> groups = Collections.singletonList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(authorizationLookup.isAdmin("bob")).thenReturn(false);
        when(securityPoliciesService.getWildcard()).thenReturn("*");

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String, Set<String>> map = new HashMap<>();
        map.put("rb1",
                Collections.singleton(securityPoliciesService.getWildcard()));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.READ)).thenReturn(map);
        when(runtimeBundleProperties.getServiceFullName()).thenReturn("rb1");

        assertThat(securityPoliciesApplicationService.canWrite("key")).isTrue();
        assertThat(securityPoliciesApplicationService.canRead("key")).isTrue();
    }

    @Test
    public void shouldHavePermissionWhenAdmin() {

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("admin");
        when(authorizationLookup.isAdmin("admin")).thenReturn(true);

        assertThat(securityPoliciesApplicationService.canWrite("key")).isTrue();
        assertThat(securityPoliciesApplicationService.canRead("key")).isTrue();
    }

    @Test
    public void shouldRestrictQueryWhenKeysFromPolicy() {
        List<String> groups = Collections.singletonList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(authorizationLookup.isAdmin("bob")).thenReturn(false);

        when(identityLookup.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String, Set<String>> map = new HashMap<>();
        map.put("rb1",
                Collections.singleton("key"));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.READ)).thenReturn(map);

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        securityPoliciesApplicationService.restrictProcessInstQuery(SecurityPolicy.READ);

        verify(processInstanceRestrictionApplier).restrictToKeys(anySet());
    }

    @Test
    public void shouldRestrictQueryWhenPoliciesButNotForUser() {

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");
        when(authorizationLookup.isAdmin("intruder")).thenReturn(false);

        when(identityLookup.getGroupsForCandidateUser("intruder")).thenReturn(null);
        Map<String, Set<String>> map = new HashMap<>();
        when(securityPoliciesService.getProcessDefinitionKeys("intruder",
                                                              null,
                                                              SecurityPolicy.READ)).thenReturn(map);

        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(query.processDefinitionId(any())).thenReturn(query);
        securityPoliciesApplicationService.restrictProcessInstQuery(SecurityPolicy.READ);

        verify(processInstanceRestrictionApplier).denyAll();
    }

    @Test
    public void shouldRestrictProcDefQueryWhenPoliciesButNotForUser() {

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");
        when(authorizationLookup.isAdmin("intruder")).thenReturn(false);

        when(identityLookup.getGroupsForCandidateUser("intruder")).thenReturn(null);
        Map<String, Set<String>> map = new HashMap<>();
        when(securityPoliciesService.getProcessDefinitionKeys("intruder",
                                                              null,
                                                              SecurityPolicy.READ)).thenReturn(map);

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(query.processDefinitionId(any())).thenReturn(query);
        securityPoliciesApplicationService.restrictProcessDefQuery(SecurityPolicy.READ);

        verify(processDefinitionRestrictionApplier).denyAll();
    }
}
