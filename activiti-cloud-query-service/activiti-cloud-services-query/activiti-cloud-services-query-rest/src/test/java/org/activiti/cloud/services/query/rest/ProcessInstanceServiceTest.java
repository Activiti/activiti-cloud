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

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.activiti.cloud.services.query.rest.specification.ProcessInstanceSpecification;
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

    private static final String TEST_USER = "testuser";
    private static final String MAIN_PROCESS_ID = "main-123";
    private static final String LINK_TYPE = "form-type";

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
    void should_linkProcess_when_initiatorsMatch() {
        // given
        ProcessInstanceEntity mainProcess = buildProcessInstanceEntity();
        mainProcess.setId(MAIN_PROCESS_ID);
        mainProcess.setInitiator(TEST_USER);

        ProcessInstanceEntity orphan = buildProcessInstanceEntity();
        orphan.setId("orphan-1");
        orphan.setInitiator(TEST_USER);
        orphan.setLinkedProcessInstanceType(LINK_TYPE);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        // when
        processInstanceService.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE);

        // then
        verify(processInstanceRepository).save(orphan);
    }

    @Test
    void should_linkProcess_when_only_matchingInitiators() {
        // given
        ProcessInstanceEntity mainProcess = buildProcessInstanceEntity();
        mainProcess.setId(MAIN_PROCESS_ID);
        mainProcess.setInitiator(TEST_USER);

        ProcessInstanceEntity orphan1 = buildProcessInstanceEntity();
        orphan1.setId("orphan-1");
        orphan1.setInitiator(TEST_USER);
        orphan1.setLinkedProcessInstanceType(LINK_TYPE);

        ProcessInstanceEntity orphan2 = buildProcessInstanceEntity();
        orphan2.setId("orphan-2");
        orphan2.setInitiator("anotherUser");
        orphan2.setLinkedProcessInstanceType(LINK_TYPE);

        ProcessInstanceEntity orphan3 = buildProcessInstanceEntity();
        orphan3.setId("orphan-3");
        orphan3.setInitiator(TEST_USER);
        orphan3.setLinkedProcessInstanceType(LINK_TYPE);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            List.of(orphan1, orphan2, orphan3)
        );

        // when
        processInstanceService.linkProcessInstances(
            MAIN_PROCESS_ID,
            List.of("orphan-1", "orphan-2", "orphan-3"),
            LINK_TYPE
        );

        // then
        verify(processInstanceRepository).save(orphan1);
        verify(processInstanceRepository).save(orphan3);
        verify(processInstanceRepository, never()).save(orphan2);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidProcessInstanceIds")
    void should_not_linkProcess_when_processInstanceIds_isInvalid(List<String> processInstanceIds) {
        // given
        ProcessInstanceEntity mainProcess = buildProcessInstanceEntity();
        mainProcess.setId(MAIN_PROCESS_ID);
        mainProcess.setInitiator(TEST_USER);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);

        // when
        processInstanceService.linkProcessInstances(MAIN_PROCESS_ID, processInstanceIds, LINK_TYPE);

        // then
        verify(processInstanceRepository, never()).findAll(any(ProcessInstanceSpecification.class));
        verify(processInstanceRepository, never()).save(any(ProcessInstanceEntity.class));
    }

    @Test
    void should_not_linkProcess_when_orphanProcesses_notFound() {
        // given
        ProcessInstanceEntity mainProcess = buildProcessInstanceEntity();
        mainProcess.setId(MAIN_PROCESS_ID);
        mainProcess.setInitiator(TEST_USER);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(
            Collections.emptyList()
        );

        List<String> processInstanceIds = List.of("orphan-1", "orphan-2");

        // when
        processInstanceService.linkProcessInstances(MAIN_PROCESS_ID, processInstanceIds, LINK_TYPE);

        // then
        verify(processInstanceRepository, never()).save(any(ProcessInstanceEntity.class));
    }

    @Test
    void should_not_linkProcess_when_initiators_dontMatch() {
        // given
        ProcessInstanceEntity mainProcess = buildProcessInstanceEntity();
        mainProcess.setId(MAIN_PROCESS_ID);
        mainProcess.setInitiator("anotherUser");

        ProcessInstanceEntity orphan = buildProcessInstanceEntity();
        orphan.setId("orphan-1");
        orphan.setInitiator(TEST_USER);
        orphan.setLinkedProcessInstanceType(LINK_TYPE);

        given(entityFinder.findById(any(), anyString(), anyString())).willReturn(mainProcess);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
        given(processInstanceRepository.findAll(any(ProcessInstanceSpecification.class))).willReturn(List.of(orphan));

        // when
        processInstanceService.linkProcessInstances(MAIN_PROCESS_ID, List.of("orphan-1"), LINK_TYPE);

        // then
        verify(processInstanceRepository, never()).save(orphan);
    }

    private static Stream<Arguments> provideInvalidProcessInstanceIds() {
        return Stream.of(Arguments.of((List<String>) null), Arguments.of(Collections.emptyList()));
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
