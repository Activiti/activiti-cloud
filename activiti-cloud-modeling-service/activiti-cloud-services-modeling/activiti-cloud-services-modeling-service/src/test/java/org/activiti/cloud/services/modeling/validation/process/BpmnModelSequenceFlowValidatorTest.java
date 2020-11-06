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
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.assertj.core.api.Assertions.assertThat;

public class BpmnModelSequenceFlowValidatorTest {

    private BpmnModelSequenceFlowValidator bpmnModelSequenceFlowValidator;

    @Mock
    private ValidationContext validationContext;

    @BeforeEach
    void setUp() {
        bpmnModelSequenceFlowValidator = new BpmnModelSequenceFlowValidator();
    }

    @Test
    public void should_returnError_when_noSourceReferenceIsSpecified() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        SequenceFlow sequenceFlow = (SequenceFlow) bpmnModel.getMainProcess().getFlowElement("testSequenceId");
        sequenceFlow.setSourceRef(null);

        assertThat(bpmnModelSequenceFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelSequenceFlowValidator.NO_SOURCE_REF_PROBLEM);
    }

    @Test
    public void should_returnError_when_noTargetReferenceIsSpecified() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        SequenceFlow sequenceFlow = (SequenceFlow) bpmnModel.getMainProcess().getFlowElement("testSequenceId");
        sequenceFlow.setTargetRef(null);

        assertThat(bpmnModelSequenceFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelSequenceFlowValidator.NO_TARGET_REF_PROBLEM);
    }

    @Test
    public void should_returnError_when_noSourceAndTargetReferenceAreSpecified() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        SequenceFlow sequenceFlow = (SequenceFlow) bpmnModel.getMainProcess().getFlowElement("testSequenceId");
        sequenceFlow.setSourceRef(null);
        sequenceFlow.setTargetRef(null);

        assertThat(bpmnModelSequenceFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelSequenceFlowValidator.NO_SOURCE_REF_PROBLEM,
                BpmnModelSequenceFlowValidator.NO_TARGET_REF_PROBLEM);
    }
}
