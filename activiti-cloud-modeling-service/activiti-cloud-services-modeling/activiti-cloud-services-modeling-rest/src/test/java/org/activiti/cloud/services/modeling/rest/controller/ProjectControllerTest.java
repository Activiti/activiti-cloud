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
package org.activiti.cloud.services.modeling.rest.controller;

import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.modeling.rest.assembler.ProjectRepresentationModelAssembler;
import org.activiti.cloud.services.modeling.service.api.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.EntityModel;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;
    @Spy
    private ProjectRepresentationModelAssembler representationModelAssembler = new ProjectRepresentationModelAssembler();

    @InjectMocks
    private ProjectController projectController;

    @Test
    void should_getProject() {
        Project projectMock = mock(Project.class);
        when(projectService.findProjectById("projectId", null)).thenReturn(Optional.of(projectMock));
        EntityModel<Project> foundProject = projectController.getProject("projectId", null);
        verify(projectService, times(1)).findProjectById("projectId", null);
        assertThat(foundProject.getContent()).isEqualTo(projectMock);
        assertThat(representationModelAssembler).isNotNull();
    }

    @Test
    void should_throwException_when_projectNotFound() {
        when(projectService.findProjectById("projectId", null)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectController.getProject("projectId", null)).isInstanceOf(ResourceNotFoundException.class);
        verify(projectService, times(1)).findProjectById("projectId", null);
    }
}
