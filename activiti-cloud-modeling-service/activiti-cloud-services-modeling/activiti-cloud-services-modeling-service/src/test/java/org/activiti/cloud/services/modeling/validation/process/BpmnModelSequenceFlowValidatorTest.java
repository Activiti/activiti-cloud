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
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class BpmnModelSequenceFlowValidatorTest {

    private BpmnModelSequenceFlowValidator bpmnModelSequenceFlowValidator;
    private final String testSequenceId = "testSequenceId";
    private final String testSequenceName = "testSequenceName";

    @Mock
    private ValidationContext validationContext;

    @BeforeEach
    void setUp() {
        bpmnModelSequenceFlowValidator = new BpmnModelSequenceFlowValidator();
    }

    @Test
    public void should_returnError_when_noSourceReferenceIsSpecified() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        SequenceFlow sequenceFlow = (SequenceFlow) bpmnModel.getMainProcess().getFlowElement(testSequenceId);
        sequenceFlow.setName(testSequenceName);
        sequenceFlow.setSourceRef(null);

        assertThat(bpmnModelSequenceFlowValidator.validate(bpmnModel, validationContext))
            .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName,
                        ModelValidationError::getReferenceId)
            .contains(tuple(BpmnModelSequenceFlowValidator.NO_SOURCE_REF_PROBLEM,
                            format(BpmnModelSequenceFlowValidator.NO_SOURCE_REF_PROBLEM_DESCRIPTION, testSequenceName, testSequenceId),
                            BpmnModelSequenceFlowValidator.SEQUENCE_FLOW_VALIDATOR_NAME,
                            testSequenceName));
    }

    @Test
    public void should_returnError_when_noTargetReferenceIsSpecified() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        SequenceFlow sequenceFlow = (SequenceFlow) bpmnModel.getMainProcess().getFlowElement(testSequenceId);
        sequenceFlow.setName(testSequenceName);
        sequenceFlow.setTargetRef(null);

        assertThat(bpmnModelSequenceFlowValidator.validate(bpmnModel, validationContext))
            .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName)
            .contains(tuple(BpmnModelSequenceFlowValidator.NO_TARGET_REF_PROBLEM,
                            format(BpmnModelSequenceFlowValidator.NO_TARGET_REF_PROBLEM_DESCRIPTION, testSequenceName, testSequenceId),
                            BpmnModelSequenceFlowValidator.SEQUENCE_FLOW_VALIDATOR_NAME));
    }
}
