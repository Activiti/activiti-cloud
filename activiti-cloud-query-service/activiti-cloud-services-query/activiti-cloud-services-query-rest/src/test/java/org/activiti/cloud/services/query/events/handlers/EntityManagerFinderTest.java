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

package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class EntityManagerFinderTest {

    @InjectMocks private EntityManagerFinder subject;

    @Mock private EntityManager entityManager;

    @Test
    void findTaskWithVariables() {
        // given
        when(entityManager.createEntityGraph(TaskEntity.class)).thenReturn(mock(EntityGraph.class));
        final TaskEntity taskEntity = mock(TaskEntity.class);
        when(entityManager.find(eq(TaskEntity.class), eq("taskId"), any(Map.class)))
                .thenReturn(taskEntity);

        // when
        Optional<TaskEntity> result = subject.findTaskWithVariables("taskId");

        // then
        assertThat(result).isNotEmpty().hasValue(taskEntity);
    }

    @Test
    void findTaskWithCandidateUsers() {
        // given
        when(entityManager.createEntityGraph(TaskEntity.class)).thenReturn(mock(EntityGraph.class));
        final TaskEntity taskEntity = mock(TaskEntity.class);
        when(entityManager.find(eq(TaskEntity.class), eq("taskId"), any(Map.class)))
                .thenReturn(taskEntity);

        // when
        Optional<TaskEntity> result = subject.findTaskWithCandidateUsers("taskId");

        // then
        assertThat(result).isNotEmpty().hasValue(taskEntity);
    }

    @Test
    void findTaskWithCandidateGroups() {
        // given
        when(entityManager.createEntityGraph(TaskEntity.class)).thenReturn(mock(EntityGraph.class));
        final TaskEntity taskEntity = mock(TaskEntity.class);
        when(entityManager.find(eq(TaskEntity.class), eq("taskId"), any(Map.class)))
                .thenReturn(taskEntity);

        // when
        Optional<TaskEntity> result = subject.findTaskWithCandidateGroups("taskId");

        // then
        assertThat(result).isNotEmpty().hasValue(taskEntity);
    }

    @Test
    void findProcessInstanceWithVariables() {
        // given
        when(entityManager.createEntityGraph(ProcessInstanceEntity.class))
                .thenReturn(mock(EntityGraph.class));
        final ProcessInstanceEntity processInstance = mock(ProcessInstanceEntity.class);
        when(entityManager.find(eq(ProcessInstanceEntity.class), eq("procId"), any(Map.class)))
                .thenReturn(processInstance);

        // when
        Optional<ProcessInstanceEntity> result = subject.findProcessInstanceWithVariables("procId");

        // then
        assertThat(result).isNotEmpty().hasValue(processInstance);
    }
}
