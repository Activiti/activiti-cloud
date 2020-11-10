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
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class IntermediateFlowNodeIncomingOutgoingFlowValidatorTest {

    private IntermediateFlowNodeIncomingOutgoingFlowValidator intermediateFlowNodeIncomingOutgoingFlowValidator;

    @BeforeEach
    void setUp() {
        intermediateFlowNodeIncomingOutgoingFlowValidator = new IntermediateFlowNodeIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_incomingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement("theTask");
        userTask.setIncomingFlows(new ArrayList<>());

        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.validate(userTask))
            .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName)
            .contains(tuple(IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM,
                            IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM_DESCRIPTION,
                            IntermediateFlowNodeIncomingOutgoingFlowValidator.INTERMEDIATE_FLOWS_VALIDATOR_NAME));
    }

    @Test
    public void should_returnError_when_outgoingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement("theTask");
        userTask.setOutgoingFlows(new ArrayList<>());

        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.validate(userTask))
            .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName)
            .contains(tuple(IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM,
                            IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION,
                            IntermediateFlowNodeIncomingOutgoingFlowValidator.INTERMEDIATE_FLOWS_VALIDATOR_NAME));
    }
}
