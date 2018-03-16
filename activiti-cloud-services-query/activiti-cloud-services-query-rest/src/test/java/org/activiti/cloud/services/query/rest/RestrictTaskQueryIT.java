package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTask;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.cloud.services.security.AuthenticationWrapper;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { TaskRepository.class, Task.class, TaskCandidateUserRepository.class, TaskCandidateUser.class, TaskCandidateGroupRepository.class, TaskCandidateGroup.class, TaskLookupRestrictionService.class})
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "org.activiti")
@EntityScan("org.activiti")
@EnableAutoConfiguration
public class RestrictTaskQueryIT {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockBean
    private AuthenticationWrapper authenticationWrapper;

    @MockBean
    private UserGroupLookupProxy userGroupLookupProxy;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldGetTasksWhenCandidate() throws Exception {

        Task task = new Task();
        task.setId("1");
        taskRepository.save(task);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("1","testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");
        when(userGroupLookupProxy.getGroupsForCandidateUser("testuser")).thenReturn(Arrays.asList("testgroup"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNotCandidate() throws Exception {

        Task task = new Task();
        task.setId("2");
        taskRepository.save(task);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("2","testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("fred");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldGetTasksWhenAssigneeEvenIfNotCandidate() throws Exception {

        Task task = new Task();
        task.setId("2");
        task.setAssignee("fred");
        taskRepository.save(task);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("2","testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("fred");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetTasksWhenInCandidateGroup() throws Exception {

        Task task = new Task();
        task.setId("3");
        taskRepository.save(task);

        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup("3","hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("hruser");
        when(userGroupLookupProxy.getGroupsForCandidateUser("hruser")).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNotInCandidateGroup() throws Exception {

        Task task = new Task();
        task.setId("4");
        taskRepository.save(task);

        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup("4","finance");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("hruser");
        when(userGroupLookupProxy.getGroupsForCandidateUser("hruser")).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldGetTasksWhenNoCandidatesConfigured() throws Exception {

        Task task = new Task();
        task.setId("5");
        taskRepository.save(task);

        // no candidates or groups - just a task without any permissions so anyone can see

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetTasksWhenNoCandidatesConfiguredAndExistingQueryMatches() throws Exception {

        Task task = new Task();
        task.setId("5");
        task.setOwner("bob");
        taskRepository.save(task);

        // no candidates or groups - just a task without any permissions so anyone can see

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTask.task.id.eq("5").and(QTask.task.owner.eq("bob")));

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNoCandidatesConfiguredAndExistingQueryDoesNotMatch() throws Exception {

        Task task = new Task();
        task.setId("5");
        task.setOwner("bob");
        taskRepository.save(task);

        // no candidates or groups - just a task without any permissions so anyone can see

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTask.task.id.eq("7").and(QTask.task.owner.eq("fred")));

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldNotGetTasksWhenInCandidateGroupButExistingQueryDoesNotMatch() throws Exception {

        Task task = new Task();
        task.setId("3");
        taskRepository.save(task);

        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup("3","hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("hruser");
        when(userGroupLookupProxy.getGroupsForCandidateUser("hruser")).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTask.task.id.eq("7"));

        Iterable<Task> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

}
