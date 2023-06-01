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
package org.activiti.cloud.services.modeling.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.entity.ProjectEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ModelRepositoryImplTest {

    private ModelRepositoryImpl repository;

    @Mock
    private ModelJpaRepository modelJpaRepository;

    @Mock
    private ProcessModelType processModelType;

    private ProjectEntity project;
    private ModelEntity model;

    @BeforeEach
    public void setUp() {
        repository = new ModelRepositoryImpl(modelJpaRepository);
        project = new ProjectEntity();
        project.setId("testProjectId");
        project.setName("testProjectName");
        model = new ModelEntity();
        model.setId("testModelId");
        model.setName("testNameId");
        model.addProject(project);
        model.addProject(new ProjectEntity());
    }

    @Test
    public void should_returnModel_when_getModelByNameInProjectAndModelExists() {
        when(
            modelJpaRepository.findModelByProjectIdAndNameEqualsAndTypeEquals(
                project.getId(),
                model.getName(),
                processModelType.getName()
            )
        )
            .thenReturn(Collections.singletonList(model));

        Optional<ModelEntity> result = repository.findModelByNameInProject(
            project,
            model.getName(),
            processModelType.getName()
        );

        verify(modelJpaRepository, times(1))
            .findModelByProjectIdAndNameEqualsAndTypeEquals(
                project.getId(),
                model.getName(),
                processModelType.getName()
            );
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(model.getId());
        assertThat(result.get().hasProjects()).isTrue();
        assertThat(result.get().hasMultipleProjects()).isTrue();
    }

    @Test
    public void should_returnModel_when_getModelByNameInProjectAndModelExistsAndProjectIsNull() {
        when(
            modelJpaRepository.findModelByProjectIdAndNameEqualsAndTypeEquals(
                null,
                model.getName(),
                processModelType.getName()
            )
        )
            .thenReturn(Collections.singletonList(model));

        Optional<ModelEntity> result = repository.findModelByNameInProject(
            null,
            model.getName(),
            processModelType.getName()
        );

        verify(modelJpaRepository, times(1))
            .findModelByProjectIdAndNameEqualsAndTypeEquals(null, model.getName(), processModelType.getName());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(model.getId());
    }

    @Test
    public void should_returnEmpty_when_getModelByNameInProjectAndModelNotExists() {
        when(
            modelJpaRepository.findModelByProjectIdAndNameEqualsAndTypeEquals(
                project.getId(),
                model.getName(),
                processModelType.getName()
            )
        )
            .thenReturn(Collections.emptyList());

        Optional<ModelEntity> result = repository.findModelByNameInProject(
            project,
            model.getName(),
            processModelType.getName()
        );

        verify(modelJpaRepository, times(1))
            .findModelByProjectIdAndNameEqualsAndTypeEquals(
                project.getId(),
                model.getName(),
                processModelType.getName()
            );
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void should_returnEmpty_when_getModelByNameInProjectAndModelNotExistsAndProjectIsNull() {
        when(
            modelJpaRepository.findModelByProjectIdAndNameEqualsAndTypeEquals(
                null,
                model.getName(),
                processModelType.getName()
            )
        )
            .thenReturn(Collections.emptyList());

        Optional<ModelEntity> result = repository.findModelByNameInProject(
            null,
            model.getName(),
            processModelType.getName()
        );

        verify(modelJpaRepository, times(1))
            .findModelByProjectIdAndNameEqualsAndTypeEquals(null, model.getName(), processModelType.getName());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void should_returnModel_when_getModelByNameAndScopeExists() {
        when(
            modelJpaRepository.findModelByNameAndScopeAndTypeEquals(
                model.getName(),
                ModelScope.GLOBAL,
                processModelType.getName()
            )
        )
            .thenReturn(Collections.singletonList(model));

        Optional<ModelEntity> result = repository.findGlobalModelByNameAndType(
            model.getName(),
            processModelType.getName()
        );

        verify(modelJpaRepository, times(1))
            .findModelByNameAndScopeAndTypeEquals(model.getName(), ModelScope.GLOBAL, processModelType.getName());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(model.getId());
    }

    @Test
    public void should_returnEmpty_when_getModelByNameAndScopeDoesNotExist() {
        when(
            modelJpaRepository.findModelByNameAndScopeAndTypeEquals(
                model.getName(),
                ModelScope.GLOBAL,
                processModelType.getName()
            )
        )
            .thenReturn(Collections.emptyList());

        Optional<ModelEntity> result = repository.findGlobalModelByNameAndType(
            model.getName(),
            processModelType.getName()
        );

        verify(modelJpaRepository, times(1))
            .findModelByNameAndScopeAndTypeEquals(model.getName(), ModelScope.GLOBAL, processModelType.getName());
        assertThat(result.isPresent()).isFalse();
    }
}
