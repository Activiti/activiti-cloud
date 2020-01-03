package org.activiti.cloud.services.modeling.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@RunWith(MockitoJUnitRunner.class)
public class ModelServiceImplTest {

    @InjectMocks
    private ModelServiceImpl modelService;

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
    private Project projectOne;

    @Mock
    private FlowElement flowElementOne;

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

    private Process initProcess(FlowElement... elements) {
        Process process = spy(new Process());
        Arrays.asList(elements)
                .forEach(process::addFlowElement);
        return process;
    }
}