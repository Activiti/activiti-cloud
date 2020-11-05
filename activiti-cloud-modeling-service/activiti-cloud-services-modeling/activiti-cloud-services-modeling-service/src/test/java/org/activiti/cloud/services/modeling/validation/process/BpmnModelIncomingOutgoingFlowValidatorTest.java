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
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class BpmnModelIncomingOutgoingFlowValidatorTest {

    private BpmnModelIncomingOutgoingFlowValidator bpmnModelIncomingOutgoingFlowValidator;

    @Mock
    private ValidationContext validationContext;

    @BeforeEach
    void setUp() {
        bpmnModelIncomingOutgoingFlowValidator = new BpmnModelIncomingOutgoingFlowValidator();
    }

    @Test
    public void should_returnError_when_startEventOutgoingFlowIsEmpty() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        StartEvent startEvent = (StartEvent) bpmnModel.getMainProcess().getFlowElement("start");
        startEvent.setOutgoingFlows(new ArrayList<>());
        SequenceFlow incomingFlow = new SequenceFlow();
        startEvent.getIncomingFlows().add(incomingFlow);

        assertThat(bpmnModelIncomingOutgoingFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM);

        assertThat(startEvent.getIncomingFlows()).isNotEmpty();
    }

    @Test
    public void should_returnError_when_startEventIncomingFlowIsNotEmpty() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        StartEvent startEvent = (StartEvent) bpmnModel.getMainProcess().getFlowElement("start");
        startEvent.setIncomingFlows(new ArrayList<>());
        SequenceFlow OutgoingFlow = new SequenceFlow();
        startEvent.getOutgoingFlows().add(OutgoingFlow);

        assertThat(bpmnModelIncomingOutgoingFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM);

        assertThat(startEvent.getOutgoingFlows()).isNotEmpty();
    }

    @Test
    public void should_returnError_when_endEventOutgoingFlowIsNotEmpty() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        EndEvent endEvent = (EndEvent) bpmnModel.getMainProcess().getFlowElement("theEnd");
        endEvent.setOutgoingFlows(new ArrayList<>());
        SequenceFlow incomingFlow = new SequenceFlow();
        endEvent.getIncomingFlows().add(incomingFlow);

        assertThat(bpmnModelIncomingOutgoingFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM);

        assertThat(endEvent.getIncomingFlows()).isNotEmpty();
    }

    @Test
    public void should_returnError_when_endEventIncomingFlowIsEmpty() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        EndEvent endEvent = (EndEvent) bpmnModel.getMainProcess().getFlowElement("theEnd");
        endEvent.setIncomingFlows(new ArrayList<>());
        SequenceFlow OutgoingFlow = new SequenceFlow();
        endEvent.getOutgoingFlows().add(OutgoingFlow);

        assertThat(bpmnModelIncomingOutgoingFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM);

        assertThat(endEvent.getOutgoingFlows()).isNotEmpty();
    }

    @Test
    public void should_returnError_when_incomingFlowIsEmpty() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement("theTask");
        userTask.setIncomingFlows(new ArrayList<>());

        assertThat(bpmnModelIncomingOutgoingFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelIncomingOutgoingFlowValidator.NO_INCOMING_FLOW_PROBLEM);
    }

    @Test
    public void should_returnError_when_outgoingFlowIsEmpty() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        UserTask userTask = (UserTask) bpmnModel.getMainProcess().getFlowElement("theTask");
        userTask.setOutgoingFlows(new ArrayList<>());

        assertThat(bpmnModelIncomingOutgoingFlowValidator.validate(bpmnModel, validationContext)).extracting("problem")
            .contains(BpmnModelIncomingOutgoingFlowValidator.NO_OUTGOING_FLOW_PROBLEM);
    }

    private static BpmnModel createOneTaskTestProcess() {
        BpmnModel model = new BpmnModel();
        org.activiti.bpmn.model.Process process = new org.activiti.bpmn.model.Process();
        model.addProcess(process);
        process.setId("oneTaskProcess");
        process.setName("The one task process");
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        process.addFlowElement(startEvent);
        UserTask userTask = new UserTask();
        userTask.setName("The Task");
        userTask.setId("theTask");
        process.addFlowElement(userTask);
        EndEvent endEvent = new EndEvent();
        endEvent.setId("theEnd");
        process.addFlowElement(endEvent);
        SequenceFlow sequenceFlow = new SequenceFlow("start", "theTask");
        sequenceFlow.setId("testSequenceId");
        process.addFlowElement(sequenceFlow);
        process.addFlowElement(new SequenceFlow("theTask", "theEnd"));
        return model;
    }
}
