/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateAggregator;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@ExtendWith(MockitoExtension.class)
public class TaskControllerHelperTest {

    private TaskControllerHelper taskControllerHelper;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Mock
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Mock
    private ProcessVariableService processVariableService;

    @Mock
    private AlfrescoPagedModelAssembler<TaskEntity> pagedCollectionModelAssembler;

    @Mock
    private QueryDslPredicateAggregator predicateAggregator;

    @Mock
    private TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    @Mock
    private PagedModel<EntityModel<QueryCloudTask>> cloudTaskPagedModel;

    @Mock
    private SecurityManager securityManager;

    @BeforeEach
    void setUp() {
        taskControllerHelper = new TaskControllerHelper(
            taskRepository,
            taskCandidateUserRepository,
            taskCandidateGroupRepository,
            processVariableService,
            pagedCollectionModelAssembler,
            predicateAggregator,
            taskRepresentationModelAssembler,
            mock(TaskLookupRestrictionService.class),
            securityManager,
            Caffeine.newBuilder().<RestrictedTaskCountCacheKey, Long>build()
        );
    }

    @AfterEach
    void resetFeatureToggles() {
        FeatureToggleHolder.reset();
    }

    @Test
    public void findAll_should_useFindByVariableNameAndValue_when_variableSearchIsSet() {
        //given
        Predicate initialPredicate = mock(Predicate.class);
        List<QueryDslPredicateFilter> filters = Collections.emptyList();
        Predicate extendedPredicate = mock(Predicate.class);
        given(predicateAggregator.applyFilters(initialPredicate, filters)).willReturn(extendedPredicate);

        VariableSearch variableSearch = new VariableSearch("var", new VariableValue<>("any"), "string");
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<TaskEntity> pageResult = new PageImpl<>(Collections.singletonList(new TaskEntity()));
        given(
            taskRepository.findByVariableNameAndValue(
                variableSearch.getName(),
                variableSearch.getValue(),
                extendedPredicate,
                pageable
            )
        ).willReturn(pageResult);

        given(pagedCollectionModelAssembler.toModel(pageable, pageResult, taskRepresentationModelAssembler)).willReturn(
            cloudTaskPagedModel
        );

        //when
        PagedModel<EntityModel<QueryCloudTask>> resultPagedModel = taskControllerHelper.findAll(
            initialPredicate,
            variableSearch,
            pageable,
            filters
        );

        //then
        assertThat(resultPagedModel).isEqualTo(cloudTaskPagedModel);
    }

    @Test
    public void findAll_should_useDefaultFindAll_when_variableSearchIsNotSet() {
        //given
        Predicate initialPredicate = mock(Predicate.class);
        List<QueryDslPredicateFilter> filters = Collections.emptyList();
        Predicate extendedPredicate = mock(Predicate.class);
        given(predicateAggregator.applyFilters(initialPredicate, filters)).willReturn(extendedPredicate);

        VariableSearch variableSearch = new VariableSearch(null, null, null);
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<TaskEntity> pageResult = new PageImpl<>(Collections.singletonList(new TaskEntity()));
        given(taskRepository.findAll(extendedPredicate, pageable)).willReturn(pageResult);

        given(pagedCollectionModelAssembler.toModel(pageable, pageResult, taskRepresentationModelAssembler)).willReturn(
            cloudTaskPagedModel
        );

        //when
        PagedModel<EntityModel<QueryCloudTask>> resultPagedModel = taskControllerHelper.findAll(
            initialPredicate,
            variableSearch,
            pageable,
            filters
        );

        //then
        assertThat(resultPagedModel).isEqualTo(cloudTaskPagedModel);
    }

    @Test
    void countTasksRestricted_should_hitRepositoryOnce_forSameUserGroupsAndRequest_whenCacheEnabled() {
        FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_TASK_COUNT_CACHE::equals);
        given(securityManager.getAuthenticatedUserId()).willReturn("test-user");
        given(securityManager.getAuthenticatedUserGroups()).willReturn(List.of("group-b", "group-a"));
        given(taskRepository.count(any(Specification.class))).willReturn(5L);

        var request = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();

        Long firstCount = taskControllerHelper.countTasksRestricted(request);
        given(securityManager.getAuthenticatedUserGroups()).willReturn(List.of("group-a", "group-b"));
        Long secondCount = taskControllerHelper.countTasksRestricted(request);

        assertThat(firstCount).isEqualTo(5L);
        assertThat(secondCount).isEqualTo(5L);
        verify(taskRepository, times(1)).count(any(Specification.class));
    }

    @Test
    void countTasksRestricted_should_missCache_whenAuthenticatedUserChanges() {
        FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_TASK_COUNT_CACHE::equals);
        given(securityManager.getAuthenticatedUserId()).willReturn("test-user", "other-user");
        given(securityManager.getAuthenticatedUserGroups()).willReturn(List.of("group-a"));
        given(taskRepository.count(any(Specification.class))).willReturn(5L, 6L);

        var request = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();

        Long firstCount = taskControllerHelper.countTasksRestricted(request);
        Long secondCount = taskControllerHelper.countTasksRestricted(request);

        assertThat(firstCount).isEqualTo(5L);
        assertThat(secondCount).isEqualTo(6L);
        verify(taskRepository, times(2)).count(any(Specification.class));
    }

    @Test
    void countTasksRestricted_should_missCache_whenAuthenticatedGroupsChange() {
        FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_TASK_COUNT_CACHE::equals);
        given(securityManager.getAuthenticatedUserId()).willReturn("test-user");
        given(securityManager.getAuthenticatedUserGroups()).willReturn(List.of("group-a"), List.of("group-b"));
        given(taskRepository.count(any(Specification.class))).willReturn(5L, 6L);

        var request = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();

        Long firstCount = taskControllerHelper.countTasksRestricted(request);
        Long secondCount = taskControllerHelper.countTasksRestricted(request);

        assertThat(firstCount).isEqualTo(5L);
        assertThat(secondCount).isEqualTo(6L);
        verify(taskRepository, times(2)).count(any(Specification.class));
    }

    @Test
    void countTasksRestricted_should_missCache_whenRequestChanges() {
        FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_TASK_COUNT_CACHE::equals);
        given(securityManager.getAuthenticatedUserId()).willReturn("test-user");
        given(securityManager.getAuthenticatedUserGroups()).willReturn(List.of("group-a"));
        given(taskRepository.count(any(Specification.class))).willReturn(5L, 6L);

        var assignedRequest = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();
        var completedRequest = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.COMPLETED).build();

        Long firstCount = taskControllerHelper.countTasksRestricted(assignedRequest);
        Long secondCount = taskControllerHelper.countTasksRestricted(completedRequest);

        assertThat(firstCount).isEqualTo(5L);
        assertThat(secondCount).isEqualTo(6L);
        verify(taskRepository, times(2)).count(any(Specification.class));
    }
}
