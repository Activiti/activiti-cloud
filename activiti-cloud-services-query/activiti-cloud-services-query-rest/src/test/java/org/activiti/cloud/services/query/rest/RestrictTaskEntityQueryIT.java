package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.runtime.api.identity.UserGroupManager;
import org.activiti.runtime.api.security.SecurityManager;
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
@SpringBootTest(classes = {TaskRepository.class, TaskEntity.class,
        TaskCandidateUserRepository.class, TaskCandidateUser.class,
        TaskCandidateGroupRepository.class, TaskCandidateGroup.class, TaskLookupRestrictionService.class})
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "org.activiti")
@EntityScan("org.activiti")
@EnableAutoConfiguration
public class RestrictTaskEntityQueryIT {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldGetTasksWhenCandidate() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("1");
        taskRepository.save(taskEntity);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("1", "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");
        when(userGroupManager.getUserGroups("testuser")).thenReturn(Arrays.asList("testgroup"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNotCandidate() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("2");
        taskRepository.save(taskEntity);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("2", "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("fred");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldGetTasksWhenAssigneeEvenIfNotCandidate() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("2");
        taskEntity.setAssignee("fred");
        taskRepository.save(taskEntity);

        TaskCandidateUser taskCandidateUser = new TaskCandidateUser("2", "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("fred");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetTasksWhenInCandidateGroup() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("3");
        taskRepository.save(taskEntity);

        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup("3", "hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(userGroupManager.getUserGroups("hruser")).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNotInCandidateGroup() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("4");
        taskRepository.save(taskEntity);

        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup("4", "finance");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(userGroupManager.getUserGroups("hruser")).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldGetTasksWhenNoCandidatesConfigured() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("5");
        taskRepository.save(taskEntity);

        // no candidates or groups - just a taskEntity without any permissions so anyone can see

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetTasksWhenNoCandidatesConfiguredAndExistingQueryMatches() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("5");
        taskEntity.setOwner("bob");
        taskRepository.save(taskEntity);

        // no candidates or groups - just a taskEntity without any permissions so anyone can see

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTaskEntity.taskEntity.id.eq("5").and(QTaskEntity.taskEntity.owner.eq("bob")));

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNoCandidatesConfiguredAndExistingQueryDoesNotMatch() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("5");
        taskEntity.setOwner("bob");
        taskRepository.save(taskEntity);

        // no candidates or groups - just a taskEntity without any permissions so anyone can see

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTaskEntity.taskEntity.id.eq("7").and(QTaskEntity.taskEntity.owner.eq("fred")));

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldNotGetTasksWhenInCandidateGroupButExistingQueryDoesNotMatch() throws Exception {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("3");
        taskRepository.save(taskEntity);

        TaskCandidateGroup taskCandidateGroup = new TaskCandidateGroup("3", "hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(userGroupManager.getUserGroups("hruser")).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTaskEntity.taskEntity.id.eq("7"));

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

}
