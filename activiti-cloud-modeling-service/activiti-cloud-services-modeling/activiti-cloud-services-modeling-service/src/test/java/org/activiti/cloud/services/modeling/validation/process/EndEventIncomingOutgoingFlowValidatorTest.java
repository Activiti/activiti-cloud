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
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EndEventIncomingOutgoingFlowValidatorTest {

    private EndEventIncomingOutgoingFlowValidator endEventIncomingOutgoingFlowValidator;
    private final String endEventId = "theEnd";
    private final String endEventName = "endEventName";

    @BeforeEach
    void setUp() {
        endEventIncomingOutgoingFlowValidator = new EndEventIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_endEventIncomingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        EndEvent endEvent = (EndEvent) bpmnModel.getMainProcess().getFlowElement(endEventId);
        endEvent.setName(endEventName);
        endEvent.setIncomingFlows(new ArrayList<>());

        assertThat(endEventIncomingOutgoingFlowValidator.validate(endEvent))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    EndEventIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM,
                    format(
                        EndEventIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM_DESCRIPTION,
                        endEventName,
                        endEventId
                    ),
                    EndEventIncomingOutgoingFlowValidator.END_EVENT_FLOWS_VALIDATOR_NAME,
                    endEventId
                )
            );
    }

    @Test
    public void should_returnError_when_endEventOutgoingFlowIsNotEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        EndEvent endEvent = (EndEvent) bpmnModel.getMainProcess().getFlowElement(endEventId);
        endEvent.setName(endEventName);
        SequenceFlow outgoingFlow = new SequenceFlow();
        endEvent.getOutgoingFlows().add(outgoingFlow);

        assertThat(endEventIncomingOutgoingFlowValidator.validate(endEvent))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    EndEventIncomingOutgoingFlowValidator.OUTGOING_FLOW_ON_END_EVENT_PROBLEM,
                    format(
                        EndEventIncomingOutgoingFlowValidator.OUTGOING_FLOW_ON_END_EVENT_PROBLEM_DESCRIPTION,
                        endEventName,
                        endEventId
                    ),
                    EndEventIncomingOutgoingFlowValidator.END_EVENT_FLOWS_VALIDATOR_NAME,
                    endEventId
                )
            );
    }

    @Test
    public void canValidate_should_returnTrue_whenItsAnEndEvent() {
        assertThat(endEventIncomingOutgoingFlowValidator.canValidate(new EndEvent())).isTrue();
    }

    @Test
    public void canValidate_should_returnFalse_whenItsNotAnEndEvent() {
        assertThat(endEventIncomingOutgoingFlowValidator.canValidate(new StartEvent())).isFalse();
    }
}
