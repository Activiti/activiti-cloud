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
package org.activiti.cloud.services.modeling.service;

import static java.util.Arrays.asList;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.activiti.cloud.modeling.api.impl.ProjectImpl;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ImportProjectException;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.activiti.cloud.services.modeling.service.api.ModelService.ProjectAccessControl;
import org.activiti.cloud.services.modeling.service.decorators.ModelExtensionsImportDecoratorService;
import org.activiti.cloud.services.modeling.service.utils.KeyGenerator;
import org.activiti.cloud.services.modeling.validation.project.ProjectNameValidator;
import org.activiti.cloud.services.modeling.validation.project.ProjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceImplTest {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private ModelService modelService;

    @Mock
    private Project project;

    @Mock
    private UserTask taskOne;

    @Mock
    private UserTask taskTwo;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private JsonConverter<Project> jsonConverter;

    @Mock
    private ModelTypeService modelTypeService;

    @Spy
    private Set<ProjectValidator> projectValidators = Set.of(new ProjectNameValidator());

    @Mock
    private Model modelOne;

    @Mock
    private KeyGenerator keyGenerator;

    @Mock
    private ModelExtensionsImportDecoratorService modelExtensionsImportDecoratorService;

    @BeforeEach
    void setUp() {
        lenient().when(keyGenerator.generate(anyString())).thenReturn("key");
    }

    @Test
    public void should_getUsersAndGroupsBelongingToAProject_when_getProcessAccessControl() {
        List<UserTask> userTasks = asList(taskOne, taskTwo);

        when(taskOne.getCandidateGroups()).thenReturn(asList("groupOne", "groupTwo"));
        when(taskOne.getCandidateUsers()).thenReturn(asList("userOne", "userTwo"));
        when(taskTwo.getAssignee()).thenReturn("userThree");
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
            .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups()).hasSize(2).contains("groupOne", "groupTwo");
        assertThat(projectAccessControl.getUsers()).hasSize(3).contains("userOne", "userTwo", "userThree");
    }

    @Test
    public void should_getUsersAndGroupsBelongingToAProjectExludingExpressions_when_getProcessAccessControl() {
        List<UserTask> userTasks = asList(taskOne, taskTwo);

        when(taskOne.getCandidateGroups()).thenReturn(asList("groupOne", "${processsVariable.groupName}", "groupTwo"));
        when(taskOne.getCandidateUsers()).thenReturn(asList("${username_Var}", "userOne"));
        when(taskTwo.getAssignee()).thenReturn("${processsVariable.username}");
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
            .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups()).hasSize(2).contains("groupOne", "groupTwo");
        assertThat(projectAccessControl.getUsers()).hasSize(1).contains("userOne");
    }

    @Test
    public void should_returnEmptyLists_when_thereAreNotAssigneeAndCandidateUsersAndGroups() {
        List<UserTask> userTasks = asList(taskOne, taskTwo);

        when(taskOne.getCandidateGroups()).thenReturn(null);
        when(taskOne.getCandidateUsers()).thenReturn(null);
        when(taskTwo.getAssignee()).thenReturn(null);
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
            .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups()).isEmpty();
        assertThat(projectAccessControl.getUsers()).isEmpty();
    }

    @Test
    public void should_returnEmptyLists_when_thereAreNotUserTasks() {
        List<UserTask> userTasks = new LinkedList<>();
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
            .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups()).isEmpty();
        assertThat(projectAccessControl.getUsers()).isEmpty();
    }

    @Test
    public void should_deleteProjectScopedModelsOnly_when_deletingAProject() {
        Model globalModel = new ModelImpl();
        globalModel.setScope(ModelScope.GLOBAL);
        globalModel.setId("global");
        Model projectModel = new ModelImpl();
        projectModel.setScope(ModelScope.PROJECT);
        projectModel.setId("project");
        when(modelService.getAllModels(project)).thenReturn(List.of(globalModel, projectModel));
        doNothing().when(modelService).deleteModel(any());
        doNothing().when(projectRepository).deleteProject(any());

        projectService.deleteProject(project);

        verify(modelService, times(1)).deleteModel(projectModel);
        verify(modelService, never()).deleteModel(globalModel);
    }

    @Test
    public void should_returnProject_importingValidProject() throws IOException {
        Project project = new ProjectImpl("id", "name", "key");
        Optional<InputStream> file = resourceAsStream("project/project-xy.zip");

        when(jsonConverter.tryConvertToEntity(any(byte[].class))).thenReturn(Optional.of(project));
        when(modelTypeService.findModelTypeByFolderName("processes")).thenReturn(Optional.of(new ProcessModelType()));
        when(projectRepository.createProject(any())).thenReturn(project);

        projectService.importProject(file.get(), "New project name");

        verify(jsonConverter, times(1)).tryConvertToEntity(any(byte[].class));
        verify(modelTypeService, times(4)).findModelTypeByFolderName("processes");
        verify(projectRepository, times(1)).createProject(any());
    }

    @Test
    public void should_throwImportProjectException_importingInvalidProject() throws IOException {
        Optional<InputStream> file = resourceAsStream("project/project-xy-invalid.zip");

        when(modelTypeService.findModelTypeByFolderName("processes")).thenReturn(Optional.of(new ProcessModelType()));

        Exception exception = assertThrows(
            ImportProjectException.class,
            () -> {
                projectService.importProject(file.get(), "new-project-name");
            }
        );
        String expectedMessage = "No valid project entry found to import";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void should_returnProject_copyingProject() {
        String copiedProjectName = "copied-project";
        String copiedProjectKey = "copied-project-key";
        Project projectToCopy = new ProjectImpl("id", copiedProjectName, copiedProjectKey);

        when(projectRepository.copyProject(projectToCopy, copiedProjectName, "key")).thenReturn(projectToCopy);
        when(modelService.getAllModels(projectToCopy)).thenReturn(asList(modelOne));
        when(modelService.copyModel(eq(modelOne), eq(projectToCopy), any())).thenReturn(modelOne);

        Project projectCopy = projectService.copyProject(projectToCopy, copiedProjectName);

        assertThat(projectCopy.getName()).isEqualTo(copiedProjectName);
        assertThat(projectCopy.getKey()).isEqualTo(copiedProjectKey);
    }

    @Test
    public void should_throwImportProjectException_replacingProjectContentWithInvalidProject() {
        Optional<InputStream> file = resourceAsStream("project/project-xy-invalid.zip");

        when(modelTypeService.findModelTypeByFolderName("processes")).thenReturn(Optional.of(new ProcessModelType()));

        Exception exception = assertThrows(
            ImportProjectException.class,
            () -> projectService.replaceProjectContentWithProvidedModelsInFile(project, file.get())
        );
        String expectedMessage = "No valid project entry found to import";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void should_deleteProjectScopedModelsOnly_when_replacingProjectContent() throws IOException {
        Model globalModel = new ModelImpl();
        globalModel.setScope(ModelScope.GLOBAL);
        globalModel.setId("global-model");

        Model projectModel = new ModelImpl();
        projectModel.setScope(ModelScope.PROJECT);
        projectModel.setId("project-model");
        when(modelService.getAllModels(project)).thenReturn(List.of(globalModel, projectModel));
        doNothing().when(modelService).deleteModel(any());

        Optional<InputStream> file = resourceAsStream("project/project-xy.zip");
        when(jsonConverter.tryConvertToEntity(any(byte[].class))).thenReturn(Optional.of(project));
        when(modelTypeService.findModelTypeByFolderName("processes")).thenReturn(Optional.of(new ProcessModelType()));

        projectService.replaceProjectContentWithProvidedModelsInFile(project, file.get());

        verify(modelService).deleteModel(projectModel);
        verify(modelService, never()).deleteModel(globalModel);
    }

    @Test
    public void should_importModelsInZipFile_when_replacingProjectContent() throws IOException {
        Optional<InputStream> file = resourceAsStream("project/project-xy.zip");

        when(jsonConverter.tryConvertToEntity(any(byte[].class))).thenReturn(Optional.of(project));
        ProcessModelType processModelType = new ProcessModelType();
        when(modelTypeService.findModelTypeByFolderName("processes")).thenReturn(Optional.of(processModelType));
        when(modelService.contentFilenameToModelName("process-x.bpmn20.xml", processModelType))
            .thenReturn(Optional.of("process-x"));
        when(modelService.contentFilenameToModelName("process-y.bpmn20.xml", processModelType))
            .thenReturn(Optional.of("process-y"));
        when(modelService.importModel(eq(project), eq(processModelType), any()))
            .thenReturn(new ImportedModel(new ModelImpl()));

        projectService.replaceProjectContentWithProvidedModelsInFile(project, file.get());

        verify(modelService, times(2)).importModel(eq(project), eq(processModelType), any());
    }

    @Test
    void should_findProjectRepresentationById() {
        ProjectImpl givenProject = new ProjectImpl("projectId", "name", "key");
        when(projectRepository.findProjectById("projectId")).thenReturn(Optional.of(givenProject));
        Optional<Project> foundProjectOptional = projectService.findProjectById("projectId", null);
        assertThat(foundProjectOptional.get()).isEqualTo(givenProject);
    }
}
