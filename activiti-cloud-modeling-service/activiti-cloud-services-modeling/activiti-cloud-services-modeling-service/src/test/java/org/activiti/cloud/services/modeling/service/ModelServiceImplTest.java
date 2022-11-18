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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelExtensionsValidator;
import org.activiti.cloud.modeling.api.ModelUpdateListener;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.activiti.cloud.modeling.api.impl.ProjectImpl;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ModelNameConflictException;
import org.activiti.cloud.modeling.core.error.ModelScopeIntegrityException;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.validation.magicnumber.FileMagicNumberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ModelServiceImplTest {

    @InjectMocks
    private ModelServiceImpl modelService;

    @Mock
    private JsonConverter jsonConverter;

    @Mock
    private ModelTypeService modelTypeService;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ProcessModelContentConverter processModelContentConverter;

    @Mock
    private Project project;

    @Mock
    private Model modelOne;

    @Mock
    private BpmnModel bpmnModelOne;

    @Mock
    private FlowElement flowElementOne;

    @Mock
    private ModelContentService modelContentService;

    @Mock
    private ModelExtensionsService modelExtensionsService;

    @Mock
    private ModelContentValidator modelContentValidator;

    @Mock
    private ModelExtensionsValidator modelExtensionsValidator;

    @Mock
    public Set<ModelUpdateListener> modelUpdateListeners;

    @Mock
    private FileMagicNumberValidator fileMagicNumberValidator;

    private Model modelTwo;

    private Project projectOne;

    private ProcessModelType modelType;

    private final String PROCESS_MODEL_TEST_CATEGORY = "test-category";

    private final String PROCESS_MODEL_DEFAULT_CATEGORY = "default-category";

    @BeforeEach
    public void setUp() {
        projectOne = new ProjectImpl();
        projectOne.setId("projectOneId");
        projectOne.setName("projectOne");

        modelType = new ProcessModelType();
        modelTwo = new ModelImpl();
        modelTwo.setId("modelTwoId");
        modelTwo.setName("name");
        modelTwo.setType(modelType.getName());
        modelTwo.setScope(ModelScope.PROJECT);
        modelTwo.setCategory(PROCESS_MODEL_DEFAULT_CATEGORY);
        modelTwo.setContent("mockContent".getBytes(StandardCharsets.UTF_8));
        modelTwo.addProject(projectOne);
    }

    @Test
    public void should_returnTasksInAProjectByModelTypeAndTaskType() throws IOException, XMLStreamException {
        UserTask userTaskOne = new UserTask();
        UserTask userTaskTwo = new UserTask();
        UserTask userTaskThree = new UserTask();
        PageImpl page = new PageImpl(asList(modelOne));
        Process processOne = initProcess(userTaskOne, flowElementOne, userTaskTwo);
        Process processTwo = initProcess(userTaskThree);

        when(modelOne.getContent()).thenReturn("".getBytes());
        when(modelRepository.getModels(any(), any(), any())).thenReturn(page);
        when(processModelContentConverter.convertToBpmnModel(any())).thenReturn(bpmnModelOne);
        when(bpmnModelOne.getProcesses()).thenReturn(asList(processOne, processTwo));

        List<UserTask> tasks = modelService.getTasksBy(project, modelType, UserTask.class);

        assertThat(tasks).hasSize(3);
        assertThat(tasks).contains(userTaskOne);
        assertThat(tasks).contains(userTaskTwo);
        assertThat(tasks).contains(userTaskThree);

        verify(processOne, times(1)).getFlowElements();
        verify(processTwo, times(1)).getFlowElements();
    }

    @Test
    public void should_returnException_when_classTypeIsNotSpecified() {
        assertThatThrownBy(() -> modelService.getTasksBy(projectOne, modelType, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Class task type it must not be null");
    }

    @Test
    public void should_returnProcessesInAProjectByTypeAndModelType() throws IOException, XMLStreamException {
        Page page = new PageImpl(asList(modelOne));
        Process processOne = new Process();

        when(modelOne.getContent()).thenReturn("".getBytes());
        when(modelService.getModels(any(), any(), any())).thenReturn(page);
        when(processModelContentConverter.convertToBpmnModel(any())).thenReturn(bpmnModelOne);
        when(bpmnModelOne.getProcesses()).thenReturn(asList(processOne));

        List<Process> processes = modelService.getProcessesBy(project, modelType);

        assertThat(processes).hasSize(1);
        assertThat(processes).contains(processOne);

        verify(bpmnModelOne, times(1)).getProcesses();
    }

    @Test
    public void should_returnProcessExtensionsFileForTheModelGiven() throws IOException, XMLStreamException {
        ModelImpl extensionModelImpl = createModelImpl();
        when(modelRepository.getModelType()).thenReturn(ModelImpl.class);
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(jsonConverter.convertToJsonBytes(any()))
            .thenReturn(new ObjectMapper().writeValueAsBytes(extensionModelImpl));

        Optional<FileContent> fileContent = modelService.getModelExtensionsFileContent(extensionModelImpl);
        assertThat(fileContent.get().getFilename()).isEqualTo("fake-process-model-extensions.json");
        assertThat(new String(fileContent.get().getFileContent()))
            .isEqualToIgnoringCase(
                "{\"id\":\"12345678\",\"name\":\"fake-process-model\",\"type\":\"PROCESS\",\"extensions\":{\"mappings\":\"\",\"constants\":\"\",\"properties\":\"\"}}"
            );
    }

    @Test
    public void should_throwModelNameConflictException_when_creatingAModelWithSameNameInAProject() {
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelRepository.findModelByNameInProject(projectOne, "name", modelType.getName()))
            .thenReturn(Optional.of(modelOne));

        when(modelOne.getId()).thenReturn("modelOneId");
        when(modelOne.getName()).thenReturn("name");
        when(modelOne.getType()).thenReturn(modelType.getName());

        assertThatThrownBy(() -> modelService.createModel(projectOne, modelTwo))
            .isInstanceOf(ModelNameConflictException.class);
    }

    @Test
    public void should_throwModelNameConflictException_when_updatingAModelWithSameNameInAProject() {
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));

        when(modelOne.getId()).thenReturn("modelOneId");
        when(modelOne.getName()).thenReturn("name");
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelRepository.findModelByNameInProject(projectOne, "name", modelType.getName()))
            .thenReturn(Optional.of(modelOne));

        assertThatThrownBy(() -> modelService.updateModel(modelTwo, modelTwo))
            .isInstanceOf(ModelNameConflictException.class);
    }

    @Test
    public void should_throwModelScopeIntegrityException_when_creatingModelWithProjectScopeAndBelongsToMoreThanOneProject()
        throws Exception {
        ModelImpl model = new ModelImpl();
        model.setScope(ModelScope.PROJECT);
        model.addProject(new ProjectImpl());
        model.addProject(new ProjectImpl());

        assertThatThrownBy(() -> modelService.createModel(null, model))
            .isInstanceOf(ModelScopeIntegrityException.class);
    }

    @Test
    public void should_throwModelScopeIntegrityException_when_updatingModelWithProjectScopeAndBelongsToMoreThanOneProject()
        throws Exception {
        ModelImpl model = new ModelImpl();
        model.setScope(ModelScope.PROJECT);
        model.addProject(new ProjectImpl());
        model.addProject(new ProjectImpl());

        assertThatThrownBy(() -> modelService.updateModel(null, model))
            .isInstanceOf(ModelScopeIntegrityException.class);
    }

    @Test
    public void should_throwException_when_validatingAnInvalidModelContentInProjectContext() throws Exception {
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelContentService.findModelValidators(modelType.getName())).thenReturn(List.of(modelContentValidator));
        doThrow(new SemanticModelValidationException(List.of(new ModelValidationError())))
            .when(modelContentValidator)
            .validateModelContent(any(), any());

        assertThatThrownBy(() -> modelService.validateModelContent(modelOne, projectOne))
            .isInstanceOf(SemanticModelValidationException.class);
    }

    @Test
    public void should_throwException_when_validatingAnInvalidModelContentFileInProjectContext() throws Exception {
        FileContent fileContent = new FileContent("testFile.txt", "txt", "".getBytes());

        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelContentService.findModelValidators(modelType.getName())).thenReturn(List.of(modelContentValidator));
        doThrow(new SemanticModelValidationException(List.of(new ModelValidationError())))
            .when(modelContentValidator)
            .validateModelContent(any(), any());

        assertThatThrownBy(() -> modelService.validateModelContent(modelOne, fileContent, projectOne))
            .isInstanceOf(SemanticModelValidationException.class);
    }

    @Test
    public void should_throwException_when_validatingAnInvalidJSONModelContentFileInProjectContext() throws Exception {
        FileContent fileContent = new FileContent("testFile.json", "application/json", "".getBytes());

        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelContentService.findModelValidators(modelType.getName())).thenReturn(List.of(modelContentValidator));
        doThrow(new SemanticModelValidationException(List.of(new ModelValidationError())))
            .when(modelContentValidator)
            .validateModelContent(any(), any());

        assertThatThrownBy(() -> modelService.validateModelContent(modelOne, fileContent, projectOne))
            .isInstanceOf(SemanticModelValidationException.class);
    }

    @Test
    public void should_throwException_when_validatingAnInvalidModelExtensionsInProjectContext() throws Exception {
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelExtensionsService.findExtensionsValidators(modelType.getName()))
            .thenReturn(List.of(modelExtensionsValidator));
        doThrow(new SemanticModelValidationException(List.of(new ModelValidationError())))
            .when(modelExtensionsValidator)
            .validateModelExtensions(any(), any());

        assertThatThrownBy(() -> modelService.validateModelExtensions(modelOne, projectOne))
            .isInstanceOf(SemanticModelValidationException.class);
    }

    @Test
    public void should_throwException_when_validatingAnInvalidModelExtensionsFileInProjectContext() throws Exception {
        FileContent fileContent = new FileContent("testFile.json", "application/json", "".getBytes());

        ConnectorModelType modelType = new ConnectorModelType();
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelExtensionsService.findExtensionsValidators(modelType.getName()))
            .thenReturn(List.of(modelExtensionsValidator));
        doThrow(new SemanticModelValidationException(List.of(new ModelValidationError())))
            .when(modelExtensionsValidator)
            .validateModelExtensions(any(), any());

        assertThatThrownBy(() -> modelService.validateModelExtensions(modelOne, fileContent, projectOne))
            .isInstanceOf(SemanticModelValidationException.class);
    }

    @Test
    public void should_throwException_when_validatingAnInvalidJSONModelExtensionsFileInProjectContext()
        throws Exception {
        FileContent fileContent = new FileContent("testFile.json", "application/json", "".getBytes());

        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelExtensionsService.findExtensionsValidators(modelType.getName()))
            .thenReturn(List.of(modelExtensionsValidator));
        doThrow(new SemanticModelValidationException(List.of(new ModelValidationError())))
            .when(modelExtensionsValidator)
            .validateModelExtensions(any(), any());

        assertThatThrownBy(() -> modelService.validateModelExtensions(modelOne, fileContent, projectOne))
            .isInstanceOf(SemanticModelValidationException.class);
    }

    @Test
    public void should_allowModelsWithAndWithoutProject_when_creatingAModelWithProject() throws Exception {
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelRepository.findModelByNameInProject(projectOne, "name", modelType.getName()))
            .thenReturn(Optional.of(modelTwo));
        when(modelRepository.createModel(modelTwo)).thenReturn(modelTwo);
        when(modelRepository.createModel(modelOne)).thenReturn(modelOne);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace(PROCESS_MODEL_TEST_CATEGORY);
        when(processModelContentConverter.convertToBpmnModel(any())).thenReturn(bpmnModel);

        when(modelOne.getId()).thenReturn("modelOneId");
        when(modelOne.getName()).thenReturn("name");
        when(modelOne.getType()).thenReturn(modelType.getName());

        assertThat(modelService.createModel(projectOne, modelTwo)).isEqualTo(modelTwo);
        assertThat(modelService.createModel(null, modelOne)).isEqualTo(modelOne);
    }

    @Test
    void should_setCategoryFromBpmnModel_whenCreatingModel() throws Exception {
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelRepository.createModel(modelTwo)).thenReturn(modelTwo);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace(PROCESS_MODEL_TEST_CATEGORY);
        when(processModelContentConverter.convertToBpmnModel(any())).thenReturn(bpmnModel);

        Model model = modelService.createModel(projectOne, modelTwo);

        assertThat(model.getCategory()).isEqualTo(PROCESS_MODEL_TEST_CATEGORY);
    }

    @Test
    void should_thrownExecption_whenUpdatingModelContentWithExecutableFile() throws Exception {
        FileContent fileContent = new FileContent("a.exe", null, "mockContent".getBytes(StandardCharsets.UTF_8));
        when(fileMagicNumberValidator.checkFileIsExecutable(any())).thenReturn(true);

        assertThatThrownBy(() -> modelService.updateModelContent(modelTwo, fileContent))
            .hasMessage("Import the executable file a.exe for type PROCESS is forbidden.");
    }

    @Test
    void should_updateCategory_whenUpdatingModelContent() throws Exception {
        FileContent fileContent = new FileContent(null, null, "mockContent".getBytes(StandardCharsets.UTF_8));
        when(modelRepository.updateModelContent(modelTwo, fileContent)).thenReturn(modelTwo);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace(PROCESS_MODEL_TEST_CATEGORY);
        when(processModelContentConverter.convertToBpmnModel(any())).thenReturn(bpmnModel);

        when(fileMagicNumberValidator.checkFileIsExecutable(any())).thenReturn(false);

        assertThat(modelTwo.getCategory()).isEqualTo(PROCESS_MODEL_DEFAULT_CATEGORY);

        Model updatedModel = modelService.updateModelContent(modelTwo, fileContent);

        assertThat(updatedModel.getCategory()).isEqualTo(PROCESS_MODEL_TEST_CATEGORY);
    }

    @Test
    public void should_updateModelSuccessfully_when_updatingAValidModelWithProject() {
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));

        when(modelOne.getId()).thenReturn("modelOneId");
        when(modelOne.getName()).thenReturn("name");
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelRepository.findModelByNameInProject(projectOne, "name", modelType.getName()))
            .thenReturn(Optional.empty());

        when(modelRepository.updateModel(modelTwo, modelTwo)).thenReturn(modelTwo);

        modelService.updateModel(modelTwo, modelTwo);

        verify(modelRepository).updateModel(modelTwo, modelTwo);
    }

    @Test
    public void should_updateModelSuccessfully_when_updatingAValidModelWithoutProject() {
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));

        when(modelOne.getId()).thenReturn("modelOneId");
        when(modelOne.getName()).thenReturn("name");
        when(modelOne.getType()).thenReturn(modelType.getName());

        when(modelRepository.findModelByNameInProject(projectOne, "name", modelType.getName()))
            .thenReturn(Optional.empty());

        Model modelThree = new ModelImpl();
        modelThree.setId("modelThreeId");
        modelThree.setName("name");
        modelThree.setType(modelType.getName());

        when(modelRepository.updateModel(modelThree, modelThree)).thenReturn(modelThree);

        modelService.updateModel(modelThree, modelThree);

        verify(modelRepository).updateModel(modelThree, modelThree);
    }

    @Test
    public void should_getModels_when_searchingByName() {
        when(modelRepository.getModelsByName(eq(projectOne), eq(modelTwo.getName()), any(Pageable.class)))
            .thenReturn(new PageImpl(asList(modelTwo)));

        Page<Model> models = modelService.getModelsByName(projectOne, modelTwo.getName(), PageRequest.of(0, 50));

        assertThat(models.getContent()).hasSize(1);
        assertThat(models.getContent().get(0)).isEqualTo(modelTwo);

        verify(modelRepository).getModelsByName(eq(projectOne), eq(modelTwo.getName()), any(Pageable.class));
    }

    @Test
    public void should_getEmptyList_when_searchingWithEmptyString() {
        Page<Model> models = modelService.getModelsByName(projectOne, "", PageRequest.of(0, 50));

        assertThat(models.getContent()).hasSize(0);

        verify(modelRepository, never()).getModelsByName(any(), any(), any());
    }

    private ModelImpl createModelImpl() {
        ModelImpl transformationModelImpl = new ModelImpl();
        LinkedHashMap extension = new LinkedHashMap<>();
        extension.put("mappings", "");
        extension.put("constants", "");
        extension.put("properties", "");
        transformationModelImpl.setExtensions(extension);
        transformationModelImpl.setName("fake-process-model");
        transformationModelImpl.setType("PROCESS");
        transformationModelImpl.setId("12345678");
        return transformationModelImpl;
    }

    private Process initProcess(FlowElement... elements) {
        Process process = spy(new Process());
        Arrays.asList(elements).forEach(process::addFlowElement);
        return process;
    }
}
