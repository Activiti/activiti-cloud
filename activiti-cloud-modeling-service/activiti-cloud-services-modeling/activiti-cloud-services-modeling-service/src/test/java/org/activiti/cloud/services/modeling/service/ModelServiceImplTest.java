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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.activiti.cloud.modeling.api.impl.ProjectImpl;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ModelNameConflictException;
import org.activiti.cloud.modeling.core.error.ModelScopeIntegrityException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

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
    private Model modelTwo;

    @Mock
    private BpmnModel bpmnModelOne;

    @Mock
    private Project projectOne;

    @Mock
    private FlowElement flowElementOne;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void should_returnTasksInAProjectByModelTypeAndTaskType() throws IOException, XMLStreamException {
        ProcessModelType modelType = new ProcessModelType();
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
        ProcessModelType modelType = new ProcessModelType();

        assertThatThrownBy(() -> modelService.getTasksBy(projectOne, modelType, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Class task type it must not be null");
    }

    @Test
    public void should_returnProcessesInAProjectByTypeAndModelType() throws IOException, XMLStreamException {
        ProcessModelType modelType = new ProcessModelType();
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
        ProcessModelType modelType = new ProcessModelType();
        ModelImpl extensionModelImpl = createModelImpl();
        when(modelRepository.getModelType()).thenReturn(ModelImpl.class);
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(jsonConverter.convertToJsonBytes(any()))
            .thenReturn(new ObjectMapper().writeValueAsBytes(extensionModelImpl));

        Optional<FileContent> fileContent = modelService.getModelExtensionsFileContent(extensionModelImpl);
        assertThat(fileContent.get().getFilename()).isEqualTo("fake-process-model-extensions.json");
        assertThat(new String(fileContent.get().getFileContent()))
            .isEqualToIgnoringCase("{\"id\":\"12345678\",\"name\":\"fake-process-model\",\"type\":\"PROCESS\",\"extensions\":{\"mappings\":\"\",\"constants\":\"\",\"properties\":\"\"}}");
    }

    @Test
    public void should_throwModelNameConflictException_when_creatingAModelWithSameNameInAProject() {
        ProcessModelType modelType = new ProcessModelType();
        when(modelTypeService.findModelTypeByName(any())).thenReturn(Optional.of(modelType));
        when(modelRepository.getModelByNameInProject(projectOne, "name", modelType.getName())).thenReturn(Optional.of(modelOne));
        when(modelTwo.getId()).thenReturn("modelTwoId");
        when(modelTwo.getName()).thenReturn("name");
        when(modelTwo.getType()).thenReturn(modelType.getName());
        when(modelOne.getId()).thenReturn("modelOneId");
        when(modelOne.getName()).thenReturn("name");
        when(modelOne.getType()).thenReturn(modelType.getName());

        assertThatThrownBy(() -> modelService.createModel(projectOne, modelTwo))
            .isInstanceOf(ModelNameConflictException.class);
    }

    @Test
    public void should_throwModelScopeIntegrityException_when_creatingModelWithProjectScopeAndBelongsToMoreThanOneProject() throws Exception {
        ModelImpl model = new ModelImpl();
        model.setScope(ModelScope.PROJECT);
        model.addProject(new ProjectImpl());
        model.addProject(new ProjectImpl());

        assertThatThrownBy(() -> modelService.createModel(null, model))
            .isInstanceOf(ModelScopeIntegrityException.class);
    }

    @Test
    public void should_throwModelScopeIntegrityException_when_updatingModelWithProjectScopeAndBelongsToMoreThanOneProject() throws Exception {
        ModelImpl model = new ModelImpl();
        model.setScope(ModelScope.PROJECT);
        model.addProject(new ProjectImpl());
        model.addProject(new ProjectImpl());

        assertThatThrownBy(() -> modelService.updateModel(null, model))
            .isInstanceOf(ModelScopeIntegrityException.class);
    }

    private ModelImpl createModelImpl() {
        ModelImpl transoformationModelImpl = new ModelImpl();
        LinkedHashMap extension = new LinkedHashMap<>();
        extension.put("mappings", "");
        extension.put("constants", "");
        extension.put("properties", "");
        transoformationModelImpl.setExtensions(extension);
        transoformationModelImpl.setName("fake-process-model");
        transoformationModelImpl.setType("PROCESS");
        transoformationModelImpl.setId("12345678");
        return transoformationModelImpl;
    }

    private Process initProcess(FlowElement... elements) {
        Process process = spy(new Process());
        Arrays.asList(elements)
                .forEach(process::addFlowElement);
        return process;
    }


}
