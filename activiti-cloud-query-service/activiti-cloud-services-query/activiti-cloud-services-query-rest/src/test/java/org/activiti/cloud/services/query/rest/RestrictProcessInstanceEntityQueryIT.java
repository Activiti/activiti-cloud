package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.querydsl.core.types.Predicate;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Iterator;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@EnableAutoConfiguration
public class RestrictProcessInstanceEntityQueryIT {

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private ProcessInstanceRestrictionService processInstanceRestrictionService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;

    @BeforeEach
    public void setUp() throws Exception {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("15");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        initMocks(this);
    }

    @Test
    public void shouldGetProcessInstancesWhenPermitted() throws Exception {

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(null, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }


    @Test
    public void shouldGetProcessInstancesWhenUserPermittedByWildcard() throws Exception {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("16");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(null, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }


    @Test
    public void shouldGetProcessInstancesWhenGroupPermittedByWildcard() throws Exception {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("17");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("bobinhr");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Collections.singletonList("hrgroup"));

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(null, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetProcessInstancesWhenPolicyNotForUser() throws Exception {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("18");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(null, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        //this user should see proc instances - but not for test-cmd-endpoint-wild

        Iterator<ProcessInstanceEntity> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            ProcessInstanceEntity proc = iterator.next();
            assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-wild");
            assertThat(proc.getServiceName()).isEqualToIgnoringCase("test-cmd-endpoint");
        }
    }

    @Test
    public void shouldMatchAppNameCaseInsensitiveIgnoringHyphens() throws Exception {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("19");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("Te-St-CmD-EnDpoInT");
        processInstanceRepository.save(processInstanceEntity);

        ProcessInstanceEntity processInstanceEntity2 = new ProcessInstanceEntity();
        processInstanceEntity2.setId("20");
        processInstanceEntity2.setName("name");
        processInstanceEntity2.setInitiator("initiator");
        processInstanceEntity2.setProcessDefinitionKey("defKey1");
        processInstanceEntity2.setServiceName("test-cmd-endpoint-dontmatchthisone");
        processInstanceRepository.save(processInstanceEntity2);

        assertThat(processInstanceRepository.count()).isGreaterThanOrEqualTo(2);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(null, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        Iterator<ProcessInstanceEntity> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            ProcessInstanceEntity proc = iterator.next();
            assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-dontmatchthisone");
            assertThat(proc.getServiceName().replace("-", "")).isEqualToIgnoringCase("test-cmd-endpoint".replace("-", ""));
        }

        assertThat(processInstanceRepository.count(predicate)).isEqualTo(2);
    }

    @Test
    public void shouldNotGetProcessInstancesWhenNotPermitted() throws Exception {

        Predicate predicate = QProcessInstanceEntity.processInstanceEntity.id.isNotNull();

        when(securityManager.getAuthenticatedUserId()).thenReturn("intruder");

        predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(predicate, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }


    @Test
    public void shouldGetProcessInstancesWhenMatchesFullServiceName() throws Exception {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("21");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKey2");
        processInstanceEntity.setServiceFullName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(null, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }
}
