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
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceAdminControllerHelper;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
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

    @Test
    void findAllProcessInstanceAdmin_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceAdminService.findAll(predicate, pageable)).willReturn(pageResult);
        given(processInstanceControllerHelper.mapAllSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdmin(
            predicate,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    void findAllProcessInstanceAdminWithVariables_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        List<String> variableKeys = Collections.singletonList("var1");
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceAdminService.findAllWithVariables(predicate, variableKeys, pageable))
            .willReturn(pageResult);
        given(processInstanceControllerHelper.mapAllSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdminWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    void findByIdProcessAdmin_shouldReturnProcessInstance() {
        //given
        String processInstanceId = "1";
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        given(processInstanceAdminService.findById(processInstanceId)).willReturn(processInstanceEntity);
        given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

        //when
        ProcessInstanceEntity result = processInstanceAdminControllerHelper.findByIdProcessAdmin(processInstanceId);

        //then
        assertThat(result).isEqualTo(processInstanceEntity);
    }

    @Test
    void searchProcessInstances_shouldReturnRootWithLinkedProcessesOfSubprocess() {
        //given
        ProcessInstanceSearchRequest searchRequest = mock(ProcessInstanceSearchRequest.class);
        Pageable pageable = PageRequest.of(0, 10);

        ProcessInstanceEntity rootProcess = new ProcessInstanceEntity();
        rootProcess.setId("root-id");

        QueryCloudSubprocessInstance subRef = new QueryCloudSubprocessInstance();
        subRef.setId("sub-id");
        rootProcess.setSubprocesses(Set.of(subRef));

        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(List.of(rootProcess));
        given(processInstanceAdminService.search(searchRequest, pageable)).willReturn(pageResult);
        given(processInstanceAdminService.searchAndMapSubProcesses(pageResult)).willReturn(pageResult);

        given(processInstanceAdminService.searchLinkedProcesses(new HashSet<>(List.of("root-id"))))
            .willReturn(List.of());

        ProcessInstanceEntity linkedToSub = new ProcessInstanceEntity();
        linkedToSub.setId("linked-to-sub-id");
        linkedToSub.setLinkedProcessInstanceId("sub-id");
        given(processInstanceAdminService.searchLinkedProcesses(Set.of("sub-id"))).willReturn(List.of(linkedToSub));

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.searchProcessInstances(
            searchRequest,
            pageable
        );

        //then
        assertThat(result.getContent()).hasSize(1);
        ProcessInstanceEntity returnedRoot = result.getContent().getFirst();
        assertThat(returnedRoot.getLinkedProcesses()).hasSize(1);
        assertThat(returnedRoot.getLinkedProcesses())
            .extracting(QueryCloudSubprocessInstance::getId)
            .containsExactly("linked-to-sub-id");
    }

    @Test
    void searchProcessInstances_shouldMergeLinkedProcessesOfRootAndSubprocess() {
        //given
        ProcessInstanceSearchRequest searchRequest = mock(ProcessInstanceSearchRequest.class);
        Pageable pageable = PageRequest.of(0, 10);

        ProcessInstanceEntity rootProcess = new ProcessInstanceEntity();
        rootProcess.setId("root-id");

        QueryCloudSubprocessInstance subRef = new QueryCloudSubprocessInstance();
        subRef.setId("sub-id");
        rootProcess.setSubprocesses(Set.of(subRef));

        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(List.of(rootProcess));
        given(processInstanceAdminService.search(searchRequest, pageable)).willReturn(pageResult);
        given(processInstanceAdminService.searchAndMapSubProcesses(pageResult)).willReturn(pageResult);

        ProcessInstanceEntity linkedToRoot = new ProcessInstanceEntity();
        linkedToRoot.setId("linked-to-root-id");
        linkedToRoot.setLinkedProcessInstanceId("root-id");
        given(processInstanceAdminService.searchLinkedProcesses(new HashSet<>(List.of("root-id"))))
            .willReturn(List.of(linkedToRoot));

        ProcessInstanceEntity linkedToSub = new ProcessInstanceEntity();
        linkedToSub.setId("linked-to-sub-id");
        linkedToSub.setLinkedProcessInstanceId("sub-id");
        given(processInstanceAdminService.searchLinkedProcesses(Set.of("sub-id"))).willReturn(List.of(linkedToSub));

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.searchProcessInstances(
            searchRequest,
            pageable
        );

        //then
        ProcessInstanceEntity returnedRoot = result.getContent().getFirst();
        assertThat(returnedRoot.getLinkedProcesses()).hasSize(2);
        assertThat(returnedRoot.getLinkedProcesses())
            .extracting(QueryCloudSubprocessInstance::getId)
            .containsExactlyInAnyOrder("linked-to-root-id", "linked-to-sub-id");
    }

    @Test
    void searchProcessInstances_shouldReturnEmptyLinkedProcesses_whenNoLinkedProcessesExist() {
        //given
        ProcessInstanceSearchRequest searchRequest = mock(ProcessInstanceSearchRequest.class);
        Pageable pageable = PageRequest.of(0, 10);

        ProcessInstanceEntity rootProcess = new ProcessInstanceEntity();
        rootProcess.setId("root-id");

        QueryCloudSubprocessInstance subRef = new QueryCloudSubprocessInstance();
        subRef.setId("sub-id");
        rootProcess.setSubprocesses(Set.of(subRef));

        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(List.of(rootProcess));
        given(processInstanceAdminService.search(searchRequest, pageable)).willReturn(pageResult);
        given(processInstanceAdminService.searchAndMapSubProcesses(pageResult)).willReturn(pageResult);

        given(processInstanceAdminService.searchLinkedProcesses(new HashSet<>(List.of("root-id"))))
            .willReturn(List.of());
        given(processInstanceAdminService.searchLinkedProcesses(Set.of("sub-id"))).willReturn(List.of());

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.searchProcessInstances(
            searchRequest,
            pageable
        );

        //then
        ProcessInstanceEntity returnedRoot = result.getContent().getFirst();
        assertThat(returnedRoot.getLinkedProcesses()).isEmpty();
    }
}
