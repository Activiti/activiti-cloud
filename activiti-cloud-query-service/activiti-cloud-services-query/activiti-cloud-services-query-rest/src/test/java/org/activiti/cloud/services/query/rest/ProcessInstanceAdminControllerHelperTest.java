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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.api.process.model.ProcessInstanceSearchResult;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceAdminControllerHelper;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceAdminControllerHelperTest {

    @InjectMocks
    private ProcessInstanceAdminControllerHelper processInstanceAdminControllerHelper;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessInstanceAdminService processInstanceAdminService;

    @Mock
    private ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Mock
    private ProcessInstanceSearchService processInstanceSearchService;

    @Nested
    class FindAllProcessInstanceAdmin {

        @Test
        void should_returnProcessInstances() {
            Predicate predicate = mock(Predicate.class);
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProcessInstanceEntity> pageResult = new PageImpl<>(
                Collections.singletonList(new ProcessInstanceEntity())
            );
            given(processInstanceAdminService.findAll(predicate, pageable)).willReturn(pageResult);
            given(processInstanceControllerHelper.mapAllSubprocesses(pageResult, pageable)).willReturn(pageResult);

            Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdmin(
                predicate,
                pageable
            );

            assertThat(result).isEqualTo(pageResult);
        }
    }

    @Nested
    class FindAllProcessInstanceAdminWithVariables {

        @Test
        void should_returnProcessInstances() {
            Predicate predicate = mock(Predicate.class);
            List<String> variableKeys = Collections.singletonList("var1");
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProcessInstanceEntity> pageResult = new PageImpl<>(
                Collections.singletonList(new ProcessInstanceEntity())
            );
            given(processInstanceAdminService.findAllWithVariables(predicate, variableKeys, pageable))
                .willReturn(pageResult);
            given(processInstanceControllerHelper.mapAllSubprocesses(pageResult, pageable)).willReturn(pageResult);

            Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdminWithVariables(
                predicate,
                variableKeys,
                pageable
            );

            assertThat(result).isEqualTo(pageResult);
        }
    }

    @Nested
    class FindByIdProcessAdmin {

        @Test
        void should_returnProcessInstance() {
            String processInstanceId = "1";
            ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
            given(processInstanceAdminService.findById(processInstanceId)).willReturn(processInstanceEntity);
            given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

            ProcessInstanceEntity result = processInstanceAdminControllerHelper.findByIdProcessAdmin(processInstanceId);

            assertThat(result).isEqualTo(processInstanceEntity);
        }
    }

    @Nested
    class SearchProcessInstances {

        @Test
        void should_mapEntitiesToResultsWithCounts() {
            Pageable pageable = PageRequest.of(0, 10);
            ProcessInstanceEntity entity = new ProcessInstanceEntity();
            entity.setId("pi-1");
            Page<ProcessInstanceEntity> pageResult = new PageImpl<>(List.of(entity));

            given(processInstanceAdminService.search(any(ProcessInstanceSearchRequest.class), eq(pageable)))
                .willReturn(pageResult);
            given(processInstanceSearchService.countRelatedProcessesByAncestor(Set.of("pi-1")))
                .willReturn(
                    Map.of(
                        "pi-1",
                        Map.of(
                            ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS,
                            4L,
                            ProcessInstanceHierarchyEntity.RELATION_LINKED,
                            7L
                        )
                    )
                );

            Page<ProcessInstanceSearchResult> result = processInstanceAdminControllerHelper.searchProcessInstances(
                new ProcessInstanceSearchRequest(),
                pageable
            );

            assertThat(result.getContent()).hasSize(1);
            ProcessInstanceSearchResult dto = result.getContent().get(0);
            assertThat(dto.getId()).isEqualTo("pi-1");
            assertThat(dto.getSubprocessesCount()).isEqualTo(4L);
            assertThat(dto.getLinkedProcessesCount()).isEqualTo(7L);
        }

        @Test
        void should_returnZeroCounts_whenAncestorHasNoDescendants() {
            Pageable pageable = PageRequest.of(0, 10);
            ProcessInstanceEntity entity = new ProcessInstanceEntity();
            entity.setId("pi-1");
            Page<ProcessInstanceEntity> pageResult = new PageImpl<>(List.of(entity));

            given(processInstanceAdminService.search(any(ProcessInstanceSearchRequest.class), eq(pageable)))
                .willReturn(pageResult);
            given(processInstanceSearchService.countRelatedProcessesByAncestor(Set.of("pi-1"))).willReturn(Map.of());

            Page<ProcessInstanceSearchResult> result = processInstanceAdminControllerHelper.searchProcessInstances(
                new ProcessInstanceSearchRequest(),
                pageable
            );

            assertThat(result.getContent()).hasSize(1);
            ProcessInstanceSearchResult dto = result.getContent().get(0);
            assertThat(dto.getSubprocessesCount()).isZero();
            assertThat(dto.getLinkedProcessesCount()).isZero();
        }

        @Test
        void should_returnEmptyPage_whenServiceReturnsNoResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.emptyList());

            given(processInstanceAdminService.search(any(ProcessInstanceSearchRequest.class), eq(pageable)))
                .willReturn(pageResult);
            given(processInstanceSearchService.countRelatedProcessesByAncestor(Set.of())).willReturn(Map.of());

            Page<ProcessInstanceSearchResult> result = processInstanceAdminControllerHelper.searchProcessInstances(
                new ProcessInstanceSearchRequest(),
                pageable
            );

            assertThat(result.getContent()).isEmpty();
        }
    }
}
