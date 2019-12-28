package org.activiti.cloud.services.modeling.converter;

import java.util.HashMap;
import java.util.Map;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.services.common.file.FileContent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProcessModelContentConverterTest {

    private ProcessModelContentConverter processModelContentConverter;
    @Mock
    private ProcessModelType processModelType;
    @Mock
    private BpmnXMLConverter bpmnXMLConverter;

    @Mock
    private FlowElement flowElement;

    @Mock
    private ReferenceIdOverrider referenceIdOverrider;

    @Before
    public void setUp() {
        processModelContentConverter = new ProcessModelContentConverter(processModelType, bpmnXMLConverter);
    }

    @Test
    public void should_notOverrideModelId_whenModelContentEmpty() {
        FileContent fileContent = mock(FileContent.class);
        byte[] emptyByteArray = new byte[0];
        given(fileContent.getFileContent()).willReturn(emptyByteArray);

        Map<String, String> modelIds = new HashMap<>();
        FileContent result = processModelContentConverter.overrideModelId(fileContent, modelIds);

        assertThat(result).isSameAs(fileContent);
    }

    @Test
    public void should_overrideAllProcessDefinition_when_newProcessId() {
        Process process = new Process();
        process.addFlowElement(flowElement);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        BpmnProcessModelContent processModelContent = new BpmnProcessModelContent(bpmnModel);

        processModelContentConverter.overrideAllProcessDefinition(processModelContent, referenceIdOverrider);

        verify(referenceIdOverrider).overrideProcessId(process);
        verify(flowElement).accept(referenceIdOverrider);
    }

}

