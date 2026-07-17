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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository.RelatedProcessCountProjection;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceSearchServiceTest {

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessVariableService processVariableService;

    @Mock
    private SecurityManager securityManager;

    @Mock
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    private ProcessInstanceSearchService service;

    @BeforeEach
    void setUp() {
        service = new ProcessInstanceSearchService(
            processInstanceRepository,
            processVariableService,
            securityManager,
            processInstanceHierarchyRepository
        );
    }

    @Nested
    class CountRelatedProcessesByAncestor {

        @Test
        void should_returnEmptyMap_whenAncestorIdsIsNull() {
            Map<String, Map<String, Long>> result = service.countRelatedProcessesByAncestor(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(processInstanceHierarchyRepository);
        }

        @Test
        void should_returnEmptyMap_whenAncestorIdsIsEmpty() {
            Map<String, Map<String, Long>> result = service.countRelatedProcessesByAncestor(Set.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(processInstanceHierarchyRepository);
        }

        @Test
        void should_groupByAncestorAndRelationType() {
            Set<String> ancestorIds = Set.of("a1", "a2");
            given(processInstanceHierarchyRepository.countRelatedByAncestor(ancestorIds)).willReturn(
                List.of(
                    projection("a1", ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS, 3L),
                    projection("a1", ProcessInstanceHierarchyEntity.RELATION_LINKED, 2L),
                    projection("a2", ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS, 1L)
                )
            );

            Map<String, Map<String, Long>> result = service.countRelatedProcessesByAncestor(ancestorIds);

            assertThat(result)
                .containsEntry(
                    "a1",
                    Map.of(
                        ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS,
                        3L,
                        ProcessInstanceHierarchyEntity.RELATION_LINKED,
                        2L
                    )
                )
                .containsEntry("a2", Map.of(ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS, 1L));
        }

        @Test
        void should_returnEmptyMap_whenRepositoryReturnsNothing() {
            Set<String> ancestorIds = Set.of("a1");
            given(processInstanceHierarchyRepository.countRelatedByAncestor(ancestorIds)).willReturn(List.of());

            Map<String, Map<String, Long>> result = service.countRelatedProcessesByAncestor(ancestorIds);

            assertThat(result).isEmpty();
        }
    }

    private static RelatedProcessCountProjection projection(String ancestorId, String relationType, long count) {
        return new RelatedProcessCountProjection() {
            @Override
            public String getAncestorId() {
                return ancestorId;
            }

            @Override
            public String getRelationType() {
                return relationType;
            }

            @Override
            public long getRelatedCount() {
                return count;
            }
        };
    }
}
