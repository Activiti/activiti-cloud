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
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class StartEventIncomingOutgoingFlowValidatorTest {

    private StartEventIncomingOutgoingFlowValidator startEventIncomingOutgoingFlowValidator;

    @Mock
    private FlowNode flowNode;

    @Mock
    private BpmnModelIncomingOutgoingFlowValidator bpmnModelIncomingOutgoingFlowValidator;

    @BeforeEach
    void setUp() {
        startEventIncomingOutgoingFlowValidator = new StartEventIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_startEventOutgoingFlowIsEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        StartEvent startEvent = (StartEvent) bpmnModel.getMainProcess().getFlowElement("start");
        startEvent.setOutgoingFlows(new ArrayList<>());
        SequenceFlow incomingFlow = new SequenceFlow();
        startEvent.getIncomingFlows().add(incomingFlow);

        assertThat(startEvent.getOutgoingFlows()).isEmpty();
        assertThat(startEventIncomingOutgoingFlowValidator.validate(flowNode, bpmnModelIncomingOutgoingFlowValidator)).extracting("problem")
            .contains(StartEventIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM);
    }

    @Test
    public void should_returnError_when_startEventIncomingFlowIsNotEmpty() {
        BpmnModel bpmnModel = CreateBpmnModelTestHelper.createOneTaskTestProcess();
        StartEvent startEvent = (StartEvent) bpmnModel.getMainProcess().getFlowElement("start");
        startEvent.setOutgoingFlows(new ArrayList<>());
        SequenceFlow incomingFlow = new SequenceFlow();
        startEvent.getIncomingFlows().add(incomingFlow);

        assertThat(startEvent.getIncomingFlows()).isNotEmpty();
        assertThat(startEventIncomingOutgoingFlowValidator.validate(flowNode, bpmnModelIncomingOutgoingFlowValidator)).extracting("problem")
            .contains(StartEventIncomingOutgoingFlowValidator.INCOMING_FLOW_ON_START_EVENT_PROBLEM);
    }
}
