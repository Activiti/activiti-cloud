package org.activiti.cloud.services.modeling.validation.process;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
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

        Model duplicateIdProcess = createProcessModelWithContent(bytesFromBpmnModelEqual);
        Model currentModelProcess = createProcessModelWithContent(bytesFromModelValidating);

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

        Model duplicateIdProcess = createProcessModelWithContent(bytesFromBpmnModelEqual);
        Model currentModelProcess = createProcessModelWithContent(bytesFromModelValidating);

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

    private Model createProcessModelWithContent(byte[] contentBytes) {
        ModelEntity processModel = new ModelEntity();
        processModel.setType("PROCESS");
        processModel.setId("random-id-cause-we-love-uuids");
        processModel.setContent(contentBytes);

        return processModel;
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
