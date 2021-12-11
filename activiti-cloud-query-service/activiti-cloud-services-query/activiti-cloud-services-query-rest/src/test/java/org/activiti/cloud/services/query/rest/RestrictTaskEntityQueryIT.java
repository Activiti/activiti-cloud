/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.querydsl.core.types.Predicate;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.UUID;

@SpringBootTest
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

    @BeforeEach
    public void setUp() {
        initMocks(this);
        taskCandidateUserRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    public void shouldGetTasksWhenCandidate() {

        TaskEntity taskEntity = new TaskEntity();
        String taskId = UUID.randomUUID().toString();
        taskEntity.setId(taskId);
        taskRepository.save(taskEntity);

        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity(taskEntity.getId(), "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("testgroup"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNotCandidate() {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("2");
        taskRepository.save(taskEntity);

        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity("2", "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("fred");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldNotGetTasksAssignedToSomeOneElseWhenCandidate() {
        //given
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("2");
        taskEntity.setAssignee("someOneElse");
        taskRepository.save(taskEntity);

        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity("2", "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        //when
        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);

        //then
        assertThat(iterable).isEmpty();
    }

    @Test
    public void shouldGetTasksAssignedToSomeOneElseWhenOwner() {
        //given
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("2");
        taskEntity.setAssignee("someOneElse");
        taskEntity.setOwner("testuser");
        taskRepository.save(taskEntity);

        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity("2", "someOneElse");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        //when
        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);

        //then
        assertThat(iterable).extracting(TaskEntity::getId).containsOnly(taskEntity.getId());
    }

    @Test
    public void shouldGetTasksWhenAssigneeEvenIfNotCandidate() {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("2");
        taskEntity.setAssignee("fred");
        taskRepository.save(taskEntity);

        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity("2", "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserId()).thenReturn("fred");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetTasksWhenInCandidateGroup() {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("3");
        taskRepository.save(taskEntity);

        TaskCandidateGroupEntity taskCandidateGroup = new TaskCandidateGroupEntity("3", "hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetTasksWhenNotInCandidateGroup() {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("4");
        taskRepository.save(taskEntity);

        TaskCandidateGroupEntity taskCandidateGroup = new TaskCandidateGroupEntity("4", "finance");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldNotGetTasksAssignedToSomeOneElseWhenInCandidateGroup() {
        //given
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("3");
        taskEntity.setAssignee("someOneElse");
        taskRepository.save(taskEntity);

        TaskCandidateGroupEntity taskCandidateGroup = new TaskCandidateGroupEntity("3", "hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        //when
        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);

        //then
        assertThat(iterable).isEmpty();
    }

    @Test
    public void shouldGetTasksWhenNoCandidatesConfiguredAndNotAssigned() {

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
    public void shouldNotGetTasksAssignedToSomeOneElseWhenNoCandidatesConfigured() {
        //given
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("5");
        taskEntity.setAssignee("someOneElse");
        taskRepository.save(taskEntity);

        // no candidates or groups - just a taskEntity without any permissions so anyone could see if not assigned

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(null);

        //when
        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);

        //then
        assertThat(iterable).isEmpty();
    }

    @Test
    public void shouldGetTasksWhenNoCandidatesConfiguredAndExistingQueryMatches() {

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
    public void shouldNotGetTasksWhenNoCandidatesConfiguredAndExistingQueryDoesNotMatch() {

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
    public void shouldNotGetTasksWhenInCandidateGroupButExistingQueryDoesNotMatch() {

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("3");
        taskRepository.save(taskEntity);

        TaskCandidateGroupEntity taskCandidateGroup = new TaskCandidateGroupEntity("3", "hr");
        taskCandidateGroupRepository.save(taskCandidateGroup);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("hr"));

        Predicate predicate = taskLookupRestrictionService.restrictTaskQuery(QTaskEntity.taskEntity.id.eq("7"));

        Iterable<TaskEntity> iterable = taskRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

}
