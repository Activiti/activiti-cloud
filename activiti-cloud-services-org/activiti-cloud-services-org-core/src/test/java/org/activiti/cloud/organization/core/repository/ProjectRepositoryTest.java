/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.organization.core.repository;

import org.activiti.cloud.organization.core.model.Project;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ProjectRepository}
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryTest {

    @Spy
    private ProjectRepository projectRepository;

    @Captor
    private ArgumentCaptor<Project> projectArgumentCaptor;

    @Test
    public void testCreateProject() {
        // GIVEN
        Project childProject = new Project("child_project_id",
                                           "Child Project");

        // WHEN
        projectRepository.createProject(childProject);

        // THEN
        verify(projectRepository,
               times(1))
                .createProject(projectArgumentCaptor.capture());

        Project createdProject = projectArgumentCaptor.getValue();
        assertThat(createdProject).isNotNull();
        assertThat(createdProject.getId()).isEqualTo("child_project_id");
        assertThat(createdProject.getName()).isEqualTo("Child Project");
    }

    @Test
    public void testUpdateProject() {
        // GIVEN
        Project projectToUpdate = new Project("project_id",
                                              "Project Name");
        Project project = new Project("new_project_id",
                                      "New Project Name");

        // WHEN
        projectRepository.updateProject(projectToUpdate,
                                        project);

        // THEN
        verify(projectRepository,
               times(1))
                .updateProject(projectArgumentCaptor.capture());

        Project updatedProject = projectArgumentCaptor.getValue();
        assertThat(updatedProject).isNotNull();
        assertThat(updatedProject.getId()).isEqualTo("project_id");
        assertThat(updatedProject.getName()).isEqualTo("New Project Name");
    }
}
