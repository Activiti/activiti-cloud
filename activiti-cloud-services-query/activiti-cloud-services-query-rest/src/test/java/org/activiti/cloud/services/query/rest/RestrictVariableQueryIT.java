package org.activiti.cloud.services.query.rest;

import java.util.Arrays;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.cloud.services.query.model.Variable;
import org.activiti.cloud.services.security.AuthenticationWrapper;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.cloud.services.security.VariableLookupRestrictionService;
import org.activiti.cloud.services.security.conf.SecurityProperties;
import org.activiti.engine.UserGroupLookupProxy;
import org.junit.After;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This is present in case of a future scenario where we need to filter task or process instance variables more generally rather than per task or per proc.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TaskRepository.class, Task.class, TaskCandidateUserRepository.class, TaskCandidateUser.class, TaskCandidateGroupRepository.class, TaskCandidateGroup.class, TaskLookupRestrictionService.class,
        ProcessInstanceRepository.class, SecurityPoliciesApplicationService.class, SecurityPoliciesService.class, SecurityProperties.class, ProcessInstance.class,
        Variable.class, VariableRepository.class, VariableLookupRestrictionService.class})
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "org.activiti")
@EntityScan("org.activiti")
@TestPropertySource("classpath:test-application.properties")
@EnableAutoConfiguration
public class RestrictVariableQueryIT {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockBean
    private AuthenticationWrapper authenticationWrapper;

    @MockBean
    private UserGroupLookupProxy userGroupLookupProxy;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableLookupRestrictionService variableLookupRestrictionService;

    @Autowired
    private VariableRepository variableRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        taskCandidateUserRepository.deleteAll();
        variableRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetTaskVariablesWhenCandidateForTask() throws Exception {

        Task task = new Task();
        task.setId("1");
        taskRepository.save(task);

        Variable variable = new Variable();
        variable.setName("name");
        variable.setValue("id");
        variable.setTaskId("1");
        variable.setTask(task);
        variableRepository.save(variable);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("1",
                                                                    "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");
        when(userGroupLookupProxy.getGroupsForCandidateUser("testuser")).thenReturn(Arrays.asList("testgroup"));

        Predicate predicate = variableLookupRestrictionService.restrictTaskVariableQuery(null);

        Iterable<Variable> iterable = variableRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetProcessInstanceVariablesWhenPermitted() throws Exception {

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId("15");
        processInstance.setName("name");
        processInstance.setDescription("desc");
        processInstance.setInitiator("initiator");
        processInstance.setProcessDefinitionKey("defKey1");
        processInstance.setApplicationName("test-cmd-endpoint");
        processInstanceRepository.save(processInstance);

        Variable variable = new Variable();
        variable.setName("name");
        variable.setValue("id");
        variable.setProcessInstanceId("15");
        variable.setProcessInstance(processInstance);
        variableRepository.save(variable);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = variableLookupRestrictionService.restrictProcessInstanceVariableQuery(null);
        Iterable<Variable> iterable = variableRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

/* The DSL queries seem to be able to join from variable to task or procInst but not both.
   Could probably do it using queryFactory approach http://www.querydsl.com/static/querydsl/latest/reference/html/ch02.html#jpa_integration
   But would then have to handle the pagination to make consistent with using repository.
   No immediate need and would be inefficient to do those joins.
 */
}
