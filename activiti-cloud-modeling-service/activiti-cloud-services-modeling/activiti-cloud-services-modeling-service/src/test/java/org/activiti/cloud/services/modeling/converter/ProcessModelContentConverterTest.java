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
package org.activiti.cloud.services.modeling.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.services.common.file.FileContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

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

    @BeforeEach
    public void setUp() {
        initMocks(this);
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
    public void should_overrideIdReferencesInFlowElements() {
        Process process = new Process();
        process.addFlowElement(flowElement);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        BpmnProcessModelContent processModelContent = new BpmnProcessModelContent(bpmnModel);

        processModelContentConverter.overrideAllProcessDefinition(processModelContent, referenceIdOverrider);

        verify(flowElement).accept(referenceIdOverrider);
    }

    @Test
    void should_overrideIdReferencesInFlowElementsFromSubprocess_when_processHasSubprocess() {
        SubProcess subProcess = new SubProcess();
        subProcess.addFlowElement(flowElement);

        Process process = new Process();
        process.addFlowElement(subProcess);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        BpmnProcessModelContent processModelContent = new BpmnProcessModelContent(bpmnModel);

        processModelContentConverter.overrideAllProcessDefinition(processModelContent, referenceIdOverrider);

        verify(flowElement).accept(referenceIdOverrider);
    }

}

