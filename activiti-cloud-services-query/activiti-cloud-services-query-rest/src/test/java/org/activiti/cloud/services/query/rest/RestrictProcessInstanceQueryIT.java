package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.QProcessInstance;
import org.activiti.cloud.services.security.AuthenticationWrapper;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.cloud.services.security.conf.SecurityProperties;
import org.activiti.engine.UserGroupLookupProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-application.properties")
@SpringBootTest(classes = { ProcessInstanceRepository.class, SecurityPoliciesApplicationService.class, SecurityPoliciesService.class, SecurityProperties.class, ProcessInstance.class})
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "org.activiti")
@EntityScan("org.activiti")
@EnableAutoConfiguration
public class RestrictProcessInstanceQueryIT {

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @MockBean
    private AuthenticationWrapper authenticationWrapper;

    @MockBean
    private UserGroupLookupProxy userGroupLookupProxy;

    @Before
    public void setUp() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("15");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKey1");
        processInstance.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstance);

        initMocks(this);
    }

    @Test
    public void shouldGetProcessInstancesWhenPermitted() throws Exception {

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(null, SecurityPolicy.READ);
        Iterable<ProcessInstance> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }


    @Test
    public void shouldGetProcessInstancesWhenUserPermittedByWildcard() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("16");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKeyWild");
        processInstance.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstance);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("hruser");

        Predicate predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(null, SecurityPolicy.READ);
        Iterable<ProcessInstance> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }


    @Test
    public void shouldGetProcessInstancesWhenGroupPermittedByWildcard() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("17");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKeyWild");
        processInstance.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstance);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bobinhr");
        when(userGroupLookupProxy.getGroupsForCandidateUser("bobinhr")).thenReturn(Collections.singletonList("hRgRoUp"));

        Predicate predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(null, SecurityPolicy.READ);
        Iterable<ProcessInstance> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetProcessInstancesWhenPolicyNotForUser() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("18");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKeyWild");
        processInstance.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstance);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(null, SecurityPolicy.READ);
        Iterable<ProcessInstance> iterable = processInstanceRepository.findAll(predicate);

        //this user should see proc instances - but not for test-cmd-endpoint-wild

        Iterator<ProcessInstance> iterator = iterable.iterator();
        while(iterator.hasNext()){
            ProcessInstance proc = iterator.next();
            assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-wild");
            assertThat(proc.getServiceName()).isEqualToIgnoringCase("test-cmd-endpoint");
        }
    }

    @Test
    public void shouldMatchAppNameCaseInsensitiveIgnoringHyphens() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("19");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKey1");
        processInstance.setServiceName("Te-St-CmD-EnDpoInT");
        processInstanceRepository.save(processInstance);

        ProcessInstance processInstance2 = new ProcessInstance();
        processInstance2.setId("20");
        processInstance2.setName("name");
        processInstance2.setDescription("desc");
        processInstance2.setInitiator("initiator");
        processInstance2.setProcessDefinitionKey("defKey1");
        processInstance2.setServiceName("test-cmd-endpoint-dontmatchthisone");
        processInstanceRepository.save(processInstance2);

        assertThat(processInstanceRepository.count()).isGreaterThanOrEqualTo(2);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(null, SecurityPolicy.READ);
        Iterable<ProcessInstance> iterable = processInstanceRepository.findAll(predicate);

        Iterator<ProcessInstance> iterator = iterable.iterator();
        while(iterator.hasNext()){
            ProcessInstance proc = iterator.next();
            assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-dontmatchthisone");
            assertThat(proc.getServiceName().replace("-","")).isEqualToIgnoringCase("test-cmd-endpoint".replace("-",""));
        }

        assertThat(processInstanceRepository.count(predicate)).isEqualTo(2);
    }

    @Test
    public void shouldNotGetProcessInstancesWhenNotPermitted() throws Exception {

        Predicate predicate = QProcessInstance.processInstance.id.isNotNull();

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("intruder");

        predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(predicate, SecurityPolicy.READ);
        Iterable<ProcessInstance> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }
}
