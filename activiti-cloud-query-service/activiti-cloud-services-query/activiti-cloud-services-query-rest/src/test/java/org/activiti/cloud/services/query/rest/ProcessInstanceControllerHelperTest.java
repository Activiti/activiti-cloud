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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
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
class ProcessInstanceControllerHelperTest {

    @InjectMocks
    private ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessInstanceService processInstanceService;

    @Test
    void findAllProcessInstances_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.findAll(predicate, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstances(
            predicate,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    void findAllProcessInstancesWithVariables_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        List<String> variableKeys = Collections.singletonList("var1");
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.findAllWithVariables(predicate, variableKeys, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstancesWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    void findById_shouldReturnProcessInstance() {
        //given
        String processInstanceId = "1";
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        given(processInstanceService.findById(processInstanceId)).willReturn(processInstanceEntity);
        given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

        //when
        ProcessInstanceEntity result = processInstanceControllerHelper.findById(processInstanceId);

        //then
        assertThat(result).isEqualTo(processInstanceEntity);
    }

    @Test
    void searchProcessInstances_shouldReturnProcessInstances() {
        //given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.search(any(ProcessInstanceSearchRequest.class), eq(pageable)))
            .willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.searchProcessInstances(
            new ProcessInstanceSearchRequestBuilder().build(),
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    void searchSubprocesses_shouldReturnSubprocesses() {
        //given
        String processInstanceId = "1";
        Predicate predicate = mock(Predicate.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.subprocesses(processInstanceId, predicate, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.searchSubprocesses(
            processInstanceId,
            predicate,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }
}
