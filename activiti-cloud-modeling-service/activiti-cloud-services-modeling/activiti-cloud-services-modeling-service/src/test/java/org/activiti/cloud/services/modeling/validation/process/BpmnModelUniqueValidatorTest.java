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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    void should_raiseAnErrorWhenMoreProcessesHaveTheSameId() {
        BpmnModel bpmnModelUnderValidation = createBPMNModelWithProcessId("Process_sharing_the_same_id");
        BpmnModel bpmnModelSharingTheSameId = createBPMNModelWithProcessId("Process_sharing_the_same_id");

        Model modelWithProcessSharingTheSameId = buildProcessModel(bpmnModelSharingTheSameId);
        Model currentModelProcess = buildProcessModel(bpmnModelUnderValidation);

        given(validationContext.getAvailableModels(any()))
            .willReturn(List.of(currentModelProcess, modelWithProcessSharingTheSameId));
        given(processModelContentConverter.convertToBpmnModel(modelWithProcessSharingTheSameId.getContent()))
            .willReturn(bpmnModelSharingTheSameId);
        given(processModelContentConverter.convertToBpmnModel(currentModelProcess.getContent()))
            .willReturn(bpmnModelUnderValidation);

        Stream<ModelValidationError> errors = bpmnModelUniqueIdValidator.validate(
            bpmnModelUnderValidation,
            validationContext
        );

        List<ModelValidationError> errorsList = errors.collect(Collectors.toList());

        assertThat(errorsList)
            .extracting(ModelValidationError::getProblem, ModelValidationError::getReferenceId)
            .contains(tuple(BpmnModelUniqueIdValidator.DUPLICATED_PROCESS_ID_ERROR, "Process_sharing_the_same_id"));
    }

    private Model buildProcessModel(BpmnModel model) {
        Model processModel = mock(Model.class);
        when(processModel.getContent()).thenReturn(model.toString().getBytes());
        return processModel;
    }

    @Test
    void should_notShowErrorsWhenMoreProcessesDoesNotHaveTheSameId() {
        BpmnModel bpmnModelUnderValidation = createBPMNModelWithProcessId("Process_under_validation");
        BpmnModel bpmnModelWithDifferentProcessId = createBPMNModelWithProcessId("Process_bpmn_equal");

        Model modelWithProcessWithDifferentId = buildProcessModel(bpmnModelWithDifferentProcessId);
        Model currentModelProcess = buildProcessModel(bpmnModelUnderValidation);

        given(validationContext.getAvailableModels(any()))
            .willReturn(List.of(currentModelProcess, modelWithProcessWithDifferentId));
        given(processModelContentConverter.convertToBpmnModel(modelWithProcessWithDifferentId.getContent()))
            .willReturn(bpmnModelWithDifferentProcessId);
        given(processModelContentConverter.convertToBpmnModel(currentModelProcess.getContent()))
            .willReturn(bpmnModelUnderValidation);

        Stream<ModelValidationError> errors = bpmnModelUniqueIdValidator.validate(
            bpmnModelUnderValidation,
            validationContext
        );
        List<ModelValidationError> errorsList = errors.collect(Collectors.toList());
        assertThat(errorsList).isEmpty();
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
