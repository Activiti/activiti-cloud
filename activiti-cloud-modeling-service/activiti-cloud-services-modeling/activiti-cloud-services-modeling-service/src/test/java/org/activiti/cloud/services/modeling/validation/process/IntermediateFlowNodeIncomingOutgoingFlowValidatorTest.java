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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.EventSubProcess;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntermediateFlowNodeIncomingOutgoingFlowValidatorTest {

    private IntermediateFlowNodeIncomingOutgoingFlowValidator intermediateFlowNodeIncomingOutgoingFlowValidator;
    private final String userTaskId = "theTask";
    private final String userTaskName = "userTaskName";

    @BeforeEach
    void setUp() {
        intermediateFlowNodeIncomingOutgoingFlowValidator = new IntermediateFlowNodeIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_incomingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement(userTaskId);
        userTask.setName(userTaskName);
        userTask.setIncomingFlows(new ArrayList<>());

        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.validate(userTask))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM,
                    format(
                        IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM_DESCRIPTION,
                        userTaskName,
                        userTaskId
                    ),
                    IntermediateFlowNodeIncomingOutgoingFlowValidator.INTERMEDIATE_FLOWS_VALIDATOR_NAME,
                    userTaskId
                )
            );
    }

    @Test
    public void should_returnError_when_outgoingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement(userTaskId);
        userTask.setName(userTaskName);
        userTask.setOutgoingFlows(new ArrayList<>());

        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.validate(userTask))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM,
                    format(
                        IntermediateFlowNodeIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION,
                        userTaskName,
                        userTaskId
                    ),
                    IntermediateFlowNodeIncomingOutgoingFlowValidator.INTERMEDIATE_FLOWS_VALIDATOR_NAME,
                    userTaskId
                )
            );
    }

    @Test
    public void canValidate_should_returnTrue_whenItsNotStartEventEndEventOrEventSubprocess() {
        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.canValidate(new UserTask())).isTrue();
    }

    @Test
    public void canValidate_should_returnTrue_whenItsASubprocess() {
        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.canValidate(new SubProcess())).isTrue();
    }

    @Test
    public void canValidate_should_returnFalse_whenItsAStartEvent() {
        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.canValidate(new StartEvent())).isFalse();
    }

    @Test
    public void canValidate_should_returnFalse_whenItsAEndEvent() {
        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.canValidate(new EndEvent())).isFalse();
    }

    @Test
    public void canValidate_should_returnFalse_whenItsAEventSubprocess() {
        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.canValidate(new EventSubProcess())).isFalse();
    }

    @Test
    public void canValidate_should_returnFalse_whenItsABoundaryEvent() {
        assertThat(intermediateFlowNodeIncomingOutgoingFlowValidator.canValidate(new BoundaryEvent())).isFalse();
    }
}
