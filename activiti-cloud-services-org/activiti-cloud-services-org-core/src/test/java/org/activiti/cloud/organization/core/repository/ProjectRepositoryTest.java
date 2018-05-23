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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.core.model.Group;
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
        Group parentGroup = new Group("parent_group_id",
                                      "Parent Group");
        Project childProject = new Project("child_project_id",
                                           "Child Project");

        // WHEN
        projectRepository.createProject(parentGroup,
                                        childProject);

        // THEN
        verify(projectRepository,
               times(1))
                .createProject(projectArgumentCaptor.capture());

        Project createdProject = projectArgumentCaptor.getValue();
        assertThat(createdProject).isNotNull();
        assertThat(createdProject.getId()).isEqualTo("child_project_id");
        assertThat(createdProject.getName()).isEqualTo("Child Project");
        assertThat(createdProject.getGroup()).isNotNull();
        assertThat(createdProject.getGroup().getId()).isEqualTo("parent_group_id");
        assertThat(createdProject.getGroup().getName()).isEqualTo("Parent Group");
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

    @Test
    public void testFindProjectByLink() {
        // GIVEN
        String projectSelfLink = "http://localhost:8080:/projects/project_id";

        // WHEN
        projectRepository.findProjectByLink(projectSelfLink);

        // THEN
        verify(projectRepository,
               times(1))
                .findProjectById(eq("project_id"));
    }

    @Test
    public void testCreateSubprojectReference() {
        // GIVEN
        Group parentGroup = new Group("parent_group_id",
                                      "Parent Group");

        Project project1 = new Project("project1",
                                          "Project 1");

        Project project2 = new Project("project2",
                                          "Project 2");

        List<String> subprojectLinks = Arrays.asList(
                "http://localhost:8080/projects/project1",
                "http://localhost:8080/projects/wrong_project_id",
                "http://localhost:8080/projects/project2"
        );

        doReturn(Optional.of(project1)).when(projectRepository).findProjectById(eq("project1"));
        doReturn(Optional.of(project2)).when(projectRepository).findProjectById(eq("project2"));

        // WHEN
        projectRepository.createProjectsReference(parentGroup,
                                                  subprojectLinks);

        // THEN
        verify(projectRepository,
               times(3))
                .findProjectById(anyString());

        verify(projectRepository,
               times(2))
                .updateProject(projectArgumentCaptor.capture());

        List<Project> updatedProjects = projectArgumentCaptor.getAllValues();
        assertThat(updatedProjects).hasSize(2);

        assertThat(updatedProjects
                           .stream()
                           .map(Project::getId)
                           .collect(Collectors.toList()))
                .containsExactly("project1",
                                 "project2");

        assertThat(updatedProjects
                           .stream()
                           .map(Project::getGroup)
                           .filter(Objects::nonNull)
                           .map(Group::getId)
                           .collect(Collectors.toList()))
                .containsExactly("parent_group_id",
                                 "parent_group_id");
    }
}
