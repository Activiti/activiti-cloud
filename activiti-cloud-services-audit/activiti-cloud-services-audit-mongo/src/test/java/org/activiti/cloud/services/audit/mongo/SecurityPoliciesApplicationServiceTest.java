package org.activiti.cloud.services.audit.mongo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.runtime.api.auth.AuthorizationLookup;
import org.activiti.runtime.api.identity.IdentityLookup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityPoliciesApplicationServiceTest {

    @InjectMocks
    @Spy
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @Mock
    private IdentityLookup userGroupLookupProxy;

    @Mock
    private AuthorizationLookup userRoleLookupProxy;

    @Mock
    private SecurityPoliciesService securityPoliciesService;

    @Mock
    private SpringSecurityAuthenticationWrapper authenticationWrapper;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldNotModifyQueryWhenNoPoliciesDefined() {
        Predicate query = mock(Predicate.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(false);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        verify(securityPoliciesApplicationService,
               times(0)).addProcessDefRestrictionToExpression(any(),
                                                              any(),
                                                              any(),
                                                              any());
    }

    @Test
    public void shouldNotModifyQueryWhenNoUser() {
        Predicate query = mock(Predicate.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn(null);

        verify(securityPoliciesApplicationService,
               times(0)).addProcessDefRestrictionToExpression(any(),
                                                              any(),
                                                              any(),
                                                              any());
    }

    @Test
    public void shouldRestrictQueryWhenGroupsAndPoliciesAvailable() {
        Predicate query = mock(Predicate.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));

        Map<String, Set<String>> policies = new HashMap<String, Set<String>>();
        policies.put("rb1",
                     new HashSet<>(Arrays.asList("SimpleProcess")));

        when(securityPoliciesService.getProcessDefinitionKeys(anyString(),
                                                              anyCollection(),
                                                              any(SecurityPolicy.class))).thenReturn(policies);

        securityPoliciesApplicationService.restrictProcessEngineEventQuery(query,
                                                                           SecurityPolicy.READ);

        verify(securityPoliciesApplicationService,
               times(1)).addProcessDefRestrictionToExpression(any(),
                                                              any(),
                                                              any(),
                                                              any());
        verify(securityPoliciesApplicationService,
               times(1)).restrictByAppNameAndProcDefKeys(any(),
                                                         any(),
                                                         any());
    }

    @Test
    public void shouldNotRestrictQueryByProcDefWhenWildcard() {
        Predicate query = mock(Predicate.class);

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(securityPoliciesService.getWildcard()).thenReturn("*");
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(Arrays.asList("hr"));

        Map<String, Set<String>> policies = new HashMap<String, Set<String>>();
        policies.put("rb1",
                     new HashSet<>(Arrays.asList(securityPoliciesService.getWildcard())));

        when(securityPoliciesService.getProcessDefinitionKeys(anyString(),
                                                              anyCollection(),
                                                              any(SecurityPolicy.class))).thenReturn(policies);

        securityPoliciesApplicationService.restrictProcessEngineEventQuery(query,
                                                                           SecurityPolicy.READ);

        //should be no proc def restriction
        verify(securityPoliciesApplicationService,
               times(0)).restrictByAppNameAndProcDefKeys(any(),
                                                         any(),
                                                         any());

        //but there should be a restriction by app name
        verify(securityPoliciesApplicationService,
               times(1)).addProcessDefRestrictionToExpression(any(),
                                                              any(),
                                                              any(),
                                                              any());
    }

    @Test
    public void shouldHavePermissionWhenDefIsInPolicy() {
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put("rb1",
                new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.WRITE)).thenReturn(map);
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.READ)).thenReturn(map);

        assertThat(securityPoliciesApplicationService.canRead("key",
                                                              "rb1")).isTrue();
    }

    @Test
    public void shouldHavePermissionWhenAdmin() {

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("admin");
        when(userRoleLookupProxy.isAdmin("admin")).thenReturn(true);

        assertThat(securityPoliciesApplicationService.canRead("key",
                                                              "rb1")).isTrue();
    }

    @Test
    public void shouldRestrictQueryWhenKeysFromPolicy() {
        List<String> groups = Arrays.asList("hr");

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
        when(userRoleLookupProxy.isAdmin("bob")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("bob")).thenReturn(groups);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put("rb1",
                new HashSet(Arrays.asList("key")));
        when(securityPoliciesService.getProcessDefinitionKeys("bob",
                                                              groups,
                                                              SecurityPolicy.READ)).thenReturn(map);

        Predicate query = mock(Predicate.class);
        securityPoliciesApplicationService.restrictProcessEngineEventQuery(query,
                                                                           SecurityPolicy.READ);

        verify(securityPoliciesApplicationService,
               times(1)).addProcessDefRestrictionToExpression(any(),
                                                              any(),
                                                              any(),
                                                              any());
    }

    @Test
    public void shouldRestrictQueryWhenPoliciesButNotForUser() {

        when(securityPoliciesService.policiesDefined()).thenReturn(true);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");
        when(userRoleLookupProxy.isAdmin("intruder")).thenReturn(false);

        when(userGroupLookupProxy.getGroupsForCandidateUser("intruder")).thenReturn(null);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();

        when(securityPoliciesService.getProcessDefinitionKeys("intruder",
                                                              null,
                                                              SecurityPolicy.READ)).thenReturn(map);

        Predicate query = mock(Predicate.class);
        securityPoliciesApplicationService.restrictProcessEngineEventQuery(query,
                                                                           SecurityPolicy.READ);

        verify(securityPoliciesApplicationService,
               times(1)).getImpossiblePredicate(any());
    }
}
