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
package org.activiti.cloud.services.modeling.validation.process;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BpmnModelUniqueValidatorTest {

    private BpmnModelUniqueIdValidator bpmnModelUniqueIdValidator;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ProcessModelType processModelType;

    @Mock
    private ProcessModelContentConverter processModelContentConverter;

    @BeforeEach
    public void setup() {
        bpmnModelUniqueIdValidator = new BpmnModelUniqueIdValidator(processModelType, processModelContentConverter);
    }

    @Test
    void should_raiseAnErrorWhenMoreProcessesHaveTheSameId() throws Exception {
        BpmnModel bpmnModelValidating = createBPMNModelWithProcessId("Process_bpmn_equal");
        byte[] bytesFromModelValidating = bpmnModelValidating.toString().getBytes();

        BpmnModel bpmnModelEqual = createBPMNModelWithProcessId("Process_bpmn_equal");
        byte[] bytesFromBpmnModelEqual = bpmnModelEqual.toString().getBytes();

        Model duplicateIdProcess = mock(Model.class);
        when(duplicateIdProcess.getContent()).thenReturn(bytesFromBpmnModelEqual);

        Model currentModelProcess = mock(Model.class);
        when(currentModelProcess.getContent()).thenReturn(bytesFromModelValidating);

        given(validationContext.getAvailableModels(any())).willReturn(List.of(currentModelProcess, duplicateIdProcess));

        given(processModelContentConverter.convertToBpmnModel(bytesFromBpmnModelEqual))
            .willReturn(bpmnModelEqual);

        given(processModelContentConverter.convertToBpmnModel(bytesFromModelValidating))
            .willReturn(bpmnModelValidating);

        Stream<ModelValidationError> errors = bpmnModelUniqueIdValidator.validate(bpmnModelValidating, validationContext);

        List<ModelValidationError> errorsList = errors.collect(Collectors.toList());

        assertThat(errorsList).isNotEmpty();
        assertThat(errorsList.size()).isEqualTo(1);
        assertThat(errorsList.get(0).getProblem()).isEqualTo(bpmnModelUniqueIdValidator.DUPLICATED_PROCESS_ID_ERROR);
        assertThat(errorsList.get(0).getReferenceId()).isEqualTo("Process_bpmn_equal");
    }

    @Test
    void should_notShowErrorsWhenMoreProcessesDoesNotHaveTheSameId() throws Exception {
        BpmnModel bpmnModelValidating = createBPMNModelWithProcessId("Process_bpmn_different");
        byte[] bytesFromModelValidating = bpmnModelValidating.toString().getBytes();

        BpmnModel bpmnModelEqual = createBPMNModelWithProcessId("Process_bpmn_equal");
        byte[] bytesFromBpmnModelEqual = bpmnModelEqual.toString().getBytes();

        Model duplicateIdProcess = mock(Model.class);
        when(duplicateIdProcess.getContent()).thenReturn(bytesFromBpmnModelEqual);

        Model currentModelProcess = mock(Model.class);
        when(currentModelProcess.getContent()).thenReturn(bytesFromModelValidating);

        given(validationContext.getAvailableModels(any())).willReturn(List.of(currentModelProcess, duplicateIdProcess));

        given(processModelContentConverter.convertToBpmnModel(bytesFromBpmnModelEqual))
            .willReturn(bpmnModelEqual);

        given(processModelContentConverter.convertToBpmnModel(bytesFromModelValidating))
            .willReturn(bpmnModelValidating);

        Stream<ModelValidationError> errors = bpmnModelUniqueIdValidator.validate(bpmnModelValidating, validationContext);

        List<ModelValidationError> errorsList = errors.collect(Collectors.toList());

        assertThat(errorsList).isEmpty();
        assertThat(errorsList.size()).isEqualTo(0);
    }

    private BpmnModel createBPMNModelWithProcessId(String processId) {
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace("");
        Process process = new Process();
        process.setId(processId);
        bpmnModel.addProcess(process);
        return bpmnModel;
    }



}
