package org.activiti.cloud.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource("classpath:propstest.properties")
public class SecurityPolicyServiceIT {

    @Autowired
    private SecurityPolicyService securityPolicyService;

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

    //cases from YAML
    @Test
    public void shouldGetProcessDefsByUserAndPoliciesYml() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys("bOb",null, Arrays.asList(SecurityPolicy.WRITE,SecurityPolicy.READ));

        assertThat(keys).hasSize(1);
        assertThat(keys).contains("TestProcess");
    }


    @Test
    public void shouldGetProcessDefsByGroupAndPoliciesYml() throws Exception {

        Collection<String> keys = securityPolicyService.getProcessDefinitionKeys(null,Arrays.asList("hr"), Arrays.asList(SecurityPolicy.READ));

        assertThat(keys).hasSize(2);
        assertThat(keys).contains("SimpleProcessYML1");
        assertThat(keys).contains("SimpleProcessYML2");
    }
}
