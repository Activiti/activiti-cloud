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
package org.activiti.cloud.services.query.app.repository;

import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CustomizedProcessInstanceRepositoryImplTest {

    private CustomizedProcessInstanceRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new CustomizedProcessInstanceRepositoryImpl();
    }

    @Test
    void testGetQueryCloudSubprocessInstance() {
        ProcessInstanceEntity subprocess = createProcessInstance(UUID.randomUUID().toString());
        QueryCloudSubprocessInstance result = repository.getQueryCloudSubprocessInstance(subprocess);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getProcessDefinitionName());
    }

    @Test
    void testGetParentIds() {
        List<ProcessInstanceEntity> processInstancesList = createParentProcessInstances(3);
        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(processInstancesList);

        List<String> result = repository.getParentIds(processInstances);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testGroupSubprocesses() {
        List<ProcessInstanceEntity> processInstancesList = createParentProcessInstances(4);
        String parentIdOne = processInstancesList.getFirst().getId();
        String parentIdTwo = processInstancesList.getLast().getId();
        List<ProcessInstanceEntity> subprocessesList = createSubprocessInstances(2, parentIdOne);
        subprocessesList.addAll(createSubprocessInstances(3, parentIdTwo));

        Page<ProcessInstanceEntity> subprocesses = new PageImpl<>(subprocessesList);

        Map<String, Set<QueryCloudSubprocessInstance>> result = repository.groupSubprocesses(subprocesses);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(parentIdOne));
        assertTrue(result.containsKey(parentIdTwo));
        assertEquals(2, result.get(parentIdOne).size());
        assertEquals(3, result.get(parentIdTwo).size());
    }

    private ProcessInstanceEntity createProcessInstance(String parentId) {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(UUID.randomUUID().toString());
        entity.setProcessDefinitionName("process-definition");
        entity.setParentId(parentId.equals("1") ? null : parentId);
        return entity;
    }

    private List<ProcessInstanceEntity> createParentProcessInstances(int count) {
        List<ProcessInstanceEntity> instances = new ArrayList<>();
        for(int i =0;i<count;i++)
        {
            instances.add(createProcessInstance("1"));
        }
        return instances;
    }

    private List<ProcessInstanceEntity> createSubprocessInstances(int count, String parentId) {
        List<ProcessInstanceEntity> instances = new ArrayList<>();
        for(int i =0;i<count;i++)
        {
            instances.add(createProcessInstance(parentId));
        }
        return instances;
    }
}
