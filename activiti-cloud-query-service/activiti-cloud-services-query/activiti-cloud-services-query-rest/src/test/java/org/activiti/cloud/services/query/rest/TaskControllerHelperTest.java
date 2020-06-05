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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateAggregator;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

public class TaskControllerHelperTest {

    @InjectMocks
    private TaskControllerHelper taskControllerHelper;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AlfrescoPagedModelAssembler<TaskEntity> pagedCollectionModelAssembler;

    @Mock
    private QueryDslPredicateAggregator predicateAggregator;

    @Mock
    private TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    @Mock
    private PagedModel<EntityModel<CloudTask>> cloudTaskPagedModel;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void findAll_should_useFindByVariableNameAndValue_when_variableSearchIsSet() {
        //given
        Predicate initialPredicate = mock(Predicate.class);
        List<QueryDslPredicateFilter> filters = Collections.emptyList();
        Predicate extendedPredicate = mock(Predicate.class);
        given(predicateAggregator.applyFilters(initialPredicate, filters)).willReturn(extendedPredicate);

        VariableSearch variableSearch = new VariableSearch("var", new VariableValue<>("any"),
            "string");
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<TaskEntity> pageResult = new PageImpl<>(Collections.singletonList(new TaskEntity()));
        given(taskRepository.findByVariableNameAndValue(variableSearch.getName(), variableSearch.getValue(),
            extendedPredicate, pageable)).willReturn(
            pageResult);

        given(pagedCollectionModelAssembler.toModel(pageable, pageResult, taskRepresentationModelAssembler))
            .willReturn(cloudTaskPagedModel);

        //when
        PagedModel<EntityModel<CloudTask>> resultPagedModel = taskControllerHelper.findAll(
            initialPredicate,
            variableSearch, pageable, filters);

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

        VariableSearch variableSearch = new VariableSearch(null, null,
            null);
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<TaskEntity> pageResult = new PageImpl<>(Collections.singletonList(new TaskEntity()));
        given(taskRepository.findAll(extendedPredicate, pageable)).willReturn(pageResult);

        given(pagedCollectionModelAssembler.toModel(pageable, pageResult, taskRepresentationModelAssembler))
            .willReturn(cloudTaskPagedModel);

        //when
        PagedModel<EntityModel<CloudTask>> resultPagedModel = taskControllerHelper.findAll(
            initialPredicate,
            variableSearch, pageable, filters);

        //then
        assertThat(resultPagedModel).isEqualTo(cloudTaskPagedModel);
    }
}
