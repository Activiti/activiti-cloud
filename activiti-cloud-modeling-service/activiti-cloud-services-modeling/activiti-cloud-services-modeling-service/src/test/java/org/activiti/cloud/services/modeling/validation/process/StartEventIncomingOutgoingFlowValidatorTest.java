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

public class StartEventIncomingOutgoingFlowValidatorTest {

    private StartEventIncomingOutgoingFlowValidator startEventIncomingOutgoingFlowValidator;
    private final String startEventId = "start";
    private final String startEventName = "startEventName";

    @BeforeEach
    void setUp() {
        startEventIncomingOutgoingFlowValidator = new StartEventIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_startEventOutgoingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        StartEvent startEvent = (StartEvent) bpmnModel.getMainProcess().getFlowElement(startEventId);
        startEvent.setName(startEventName);
        startEvent.setOutgoingFlows(new ArrayList<>());

        assertThat(startEventIncomingOutgoingFlowValidator.validate(startEvent))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    StartEventIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM,
                    format(
                        StartEventIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION,
                        startEventName,
                        startEventId
                    ),
                    StartEventIncomingOutgoingFlowValidator.START_EVENT_FLOWS_VALIDATOR_NAME,
                    startEventId
                )
            );
    }

    @Test
    public void should_returnError_when_startEventIncomingFlowIsNotEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        StartEvent startEvent = (StartEvent) bpmnModel.getMainProcess().getFlowElement(startEventId);
        startEvent.setName(startEventName);
        SequenceFlow incomingFlow = new SequenceFlow();
        startEvent.getIncomingFlows().add(incomingFlow);

        assertThat(startEventIncomingOutgoingFlowValidator.validate(startEvent))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    StartEventIncomingOutgoingFlowValidator.INCOMING_FLOW_ON_START_EVENT_PROBLEM,
                    format(
                        StartEventIncomingOutgoingFlowValidator.INCOMING_FLOW_ON_START_EVENT_PROBLEM_DESCRIPTION,
                        startEventName,
                        startEventId
                    ),
                    StartEventIncomingOutgoingFlowValidator.START_EVENT_FLOWS_VALIDATOR_NAME,
                    startEventId
                )
            );
    }

    @Test
    public void canValidate_should_returnTrue_when_itsAStartEvent() {
        assertThat(startEventIncomingOutgoingFlowValidator.canValidate(new StartEvent())).isTrue();
    }

    @Test
    public void canValidate_should_returnTrue_when_itsNotAStartEvent() {
        assertThat(startEventIncomingOutgoingFlowValidator.canValidate(new EndEvent())).isFalse();
    }
}
