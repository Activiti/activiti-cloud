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
import org.springframework.test.context.junit4.SpringRunner;

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

    @Before
    public void setUp() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("15");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKey1");
        processInstance.setApplicationName("test-cmd-endpoint");
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
    public void shouldMatchAppNameCaseInsensitiveIgnoringHyphens() throws Exception {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("16");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKey1");
        processInstance.setApplicationName("Te-St-CmD-EnDpoInT");
        processInstanceRepository.save(processInstance);

        assertThat(processInstanceRepository.count()).isEqualTo(2);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(null, SecurityPolicy.READ);

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
