package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.querydsl.core.types.Predicate;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.security.ProcessVariableLookupRestrictionService;
import org.activiti.cloud.services.security.TaskVariableLookupRestrictionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

/**
 * This is present in case of a future scenario where we need to filter task or process instance variables more generally rather than per task or per proc.
 */
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public class RestrictVariableEntityQueryIT {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private ProcessVariableLookupRestrictionService processVariableLookupRestrictionService;

    @Autowired
    private TaskVariableLookupRestrictionService taskVariableLookupRestrictionService;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskVariableRepository taskVariableRepository;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        taskCandidateUserRepository.deleteAll();
        variableRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldGetTaskVariablesWhenCandidateForTask() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("1");
        taskRepository.save(taskEntity);

        TaskVariableEntity variableEntity = new TaskVariableEntity();
        variableEntity.setName("name");
        variableEntity.setValue("id");
        variableEntity.setTaskId("1");
        variableEntity.setTask(taskEntity);
        taskVariableRepository.save(variableEntity);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("1",
                "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("testgroup"));

        Predicate predicate = taskVariableLookupRestrictionService.restrictTaskVariableQuery(null);

        Iterable<TaskVariableEntity> iterable = taskVariableRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetProcessInstanceVariablesWhenPermitted() throws Exception {

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("15");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        ProcessVariableEntity variableEntity = new ProcessVariableEntity();
        variableEntity.setName("name");
        variableEntity.setValue("id");
        variableEntity.setProcessInstanceId("15");
        variableEntity.setProcessInstance(processInstanceEntity);
        variableRepository.save(variableEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processVariableLookupRestrictionService.restrictProcessInstanceVariableQuery(null);
        Iterable<ProcessVariableEntity> iterable = variableRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

/* The DSL queries seem to be able to join from variable to task or procInst but not both.
   Could probably do it using queryFactory approach http://www.querydsl.com/static/querydsl/latest/reference/html/ch02.html#jpa_integration
   But would then have to handle the pagination to make consistent with using repository.
   No immediate need and would be inefficient to do those joins.
   Better solution would be to add application name and process definition to task and/or variable to avoid the joins.
   Should now also be able to simplify mapping using ElementCollection
 */
}
