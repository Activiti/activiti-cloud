package org.activiti.cloud.services;

import org.activiti.conf.SecurityProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SecurityPolicyServiceTest {

    @InjectMocks
    private SecurityPolicyService securityPolicyService;

    @Mock
    private SecurityProperties securityProperties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        HashMap<String,String> group = new HashMap<>();
        group.put("finance.policy.read","SimpleProcess1,SimpleProcess2");
        group.put("hr.policy.read","SimpleProcessYML1,SimpleProcessYML2");

        HashMap<String,String> user = new HashMap<>();
        user.put("jeff.policy.write","SimpleProcess");
        user.put("fredslinehasanerror.policy.","SimpleProcess");
        user.put("jimhasnothing.policy.read","");
        user.put("bob.policy.read","TestProcess");

        when(securityProperties.getGroup()).thenReturn(group);
        when(securityProperties.getUser()).thenReturn(user);

    }

    @Test
    public void shouldBePoliciesDefined() throws Exception {
        assertThat(securityPolicyService.policiesDefined()).isTrue();
    }


    @Test
    public void shouldGetProcessDefsByUserAndPolicies() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys("jEff",null, Arrays.asList(SecurityPolicy.WRITE,SecurityPolicy.READ));

        assertThat(keys).hasSize(1);
        assertThat(keys).contains("SimpleProcess");
    }

    @Test
    public void shouldGetProcessDefsByUserAndMinPolicy() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys("jEff",null, SecurityPolicy.READ);

        assertThat(keys).hasSize(1);
        assertThat(keys).contains("SimpleProcess");

        //write as min policy should work too for this case
        keys = securityPolicyService.getProcessDefinitionKeys("jEff",null, SecurityPolicy.WRITE);

        assertThat(keys).contains("SimpleProcess");
    }

    @Test
    public void shouldGetProcessDefsByGroupAndPolicies() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys(null,Arrays.asList("finance"), Arrays.asList(SecurityPolicy.READ));

        assertThat(keys).hasSize(2);
        assertThat(keys).contains("SimpleProcess1","SimpleProcess2");
    }

    @Test
    public void shouldGetProcessDefsByGroupsAndMinPolicy() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys(null,Arrays.asList("finance","nonexistent"), SecurityPolicy.READ);

        assertThat(keys).hasSize(2);
        assertThat(keys).contains("SimpleProcess1","SimpleProcess2");
    }

    @Test
    public void shouldNotGetProcessDefsForGroupWithoutDefs() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys(null,Arrays.asList("hrbitlikerealgroupbutnot","nonexistent"), SecurityPolicy.READ);

        assertThat(keys).isEmpty();
    }

    @Test
    public void shouldNotGetProcessDefsWithoutUserOrGroup() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys(null,null, Arrays.asList(SecurityPolicy.WRITE));

        assertThat(keys).isEmpty();
    }

    @Test
    public void shouldNotGetProcessDefsWithoutPolicyLevels() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys(null,Arrays.asList("finance"), new HashSet<>());

        assertThat(keys).isEmpty();
    }

    @Test
    public void shouldNotGetProcessDefsWhenEntryMissingPolicyLevels() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys("fredslinehasanerror", null, SecurityPolicy.READ);
        assertThat(keys).isEmpty();
    }

    @Test
    public void shouldNotGetProcessDefsWhenEntryMissingProcDefKeys() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys("jimhasnothing", null, SecurityPolicy.READ);
        assertThat(keys).isEmpty();
    }
}
