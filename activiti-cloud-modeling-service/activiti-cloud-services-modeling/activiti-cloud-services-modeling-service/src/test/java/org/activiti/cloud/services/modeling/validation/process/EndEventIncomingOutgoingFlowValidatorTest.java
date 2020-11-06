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
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class EndEventIncomingOutgoingFlowValidatorTest {

    private EndEventIncomingOutgoingFlowValidator endEventIncomingOutgoingFlowValidator;

    @BeforeEach
    void setUp() {
        endEventIncomingOutgoingFlowValidator = new EndEventIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_endEventIncomingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        EndEvent endEvent = (EndEvent) bpmnModel.getMainProcess().getFlowElement("theEnd");
        endEvent.setIncomingFlows(new ArrayList<>());

        assertThat(endEventIncomingOutgoingFlowValidator.validate(endEvent)).extracting("problem")
            .contains(EndEventIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM);
    }

    @Test
    public void should_returnError_when_endEventOutgoingFlowIsNotEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        EndEvent endEvent = (EndEvent) bpmnModel.getMainProcess().getFlowElement("theEnd");
        SequenceFlow outgoingFlow = new SequenceFlow();
        endEvent.getOutgoingFlows().add(outgoingFlow);

        assertThat(endEventIncomingOutgoingFlowValidator.validate(endEvent)).extracting("problem")
            .contains(EndEventIncomingOutgoingFlowValidator.OUTGOING_FLOW_ON_END_EVENT_PROBLEM);
    }

}
