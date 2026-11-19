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
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceServiceTest {

    private ProcessInstanceService processInstanceService;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProcessInstanceSearchService processInstanceSearchService;

    @Mock
    private ProcessInstanceRestrictionService processInstanceRestrictionService;

    @Mock
    private SecurityPoliciesManager securityPoliciesApplicationService;

    @Mock
    private SecurityManager securityManager;

    @Mock
    private EntityFinder entityFinder;

    @BeforeEach
    void setUp() {
        processInstanceService = new ProcessInstanceService(
            processInstanceRepository,
            taskRepository,
            processInstanceSearchService,
            processInstanceRestrictionService,
            securityPoliciesApplicationService,
            securityManager,
            entityFinder
        );
    }

    @Test
    void should_returnSamePage_when_searchSubProcesses_givenEmptyPage() {
        // given
        Page<ProcessInstanceEntity> emptyPage = new PageImpl<>(Collections.emptyList());

        // when
        Page<ProcessInstanceEntity> result = processInstanceService.searchSubProcesses(emptyPage);

        // then
        assertThat(result).isSameAs(emptyPage);
        verifyNoInteractions(processInstanceRepository);
        verifyNoInteractions(securityManager);
    }

    @ParameterizedTest
    @MethodSource("provideEmptyLinkedProcessInstanceIds")
    void should_returnEmptyList_when_searchLinkedProcesses_givenEmptyOrNullIds(Set<String> ids) {
        // when
        List<ProcessInstanceEntity> result = processInstanceService.searchLinkedProcesses(ids);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(processInstanceRepository);
        verifyNoInteractions(securityManager);
    }

    private static Stream<Arguments> provideEmptyLinkedProcessInstanceIds() {
        return Stream.of(Arguments.of((Set<String>) null), Arguments.of(Collections.emptySet()));
    }
}
