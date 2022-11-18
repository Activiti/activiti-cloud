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
package org.activiti.cloud.services.core.pageable;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

import org.activiti.cloud.services.core.pageable.sort.TaskSortApplier;
import org.activiti.cloud.services.core.utils.MockUtils;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.TaskQueryProperty;
import org.activiti.engine.task.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
public class TaskSortApplierTest {

    @InjectMocks
    private TaskSortApplier sortApplier;

    @Test
    public void applySort_should_oder_by_task_id_asc_by_default() throws Exception {
        //given
        TaskQuery query = MockUtils.selfReturningMock(TaskQuery.class);
        PageRequest pageRequest = PageRequest.of(0, 10);

        //when
        sortApplier.applySort(query, pageRequest);

        //then
        verify(query).orderByTaskId();
        verify(query).asc();
    }

    @Test
    public void applySort_should_use_the_criteria_defined_by_pageable_object() throws Exception {
        //given
        TaskQuery query = MockUtils.selfReturningMock(TaskQuery.class);
        Sort.Order processDefinitionOrder = new Sort.Order(Sort.Direction.ASC, "name");
        Sort.Order processInstanceOrder = new Sort.Order(Sort.Direction.DESC, "id");
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(processDefinitionOrder, processInstanceOrder));

        //when
        sortApplier.applySort(query, pageRequest);

        //then
        InOrder inOrder = inOrder(query);
        inOrder.verify(query).orderBy(TaskQueryProperty.NAME);
        inOrder.verify(query).asc();
        inOrder.verify(query).orderBy(TaskQueryProperty.TASK_ID);
        inOrder.verify(query).desc();
    }

    @Test
    public void applySort_should_throw_exception_when_using_invalid_property_to_sort() throws Exception {
        //given
        TaskQuery query = MockUtils.selfReturningMock(TaskQuery.class);
        Sort.Order invalidProperty = new Sort.Order(Sort.Direction.ASC, "invalidProperty");
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(invalidProperty));

        //then
        //when
        assertThatExceptionOfType(ActivitiIllegalArgumentException.class)
            .isThrownBy(() -> sortApplier.applySort(query, pageRequest))
            .withMessageContaining("invalidProperty");
    }
}
