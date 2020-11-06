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
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementation of {@link BpmnModelValidator} for validating Incoming and Outgoing flows
 */
public class BpmnModelIncomingOutgoingFlowValidator implements BpmnModelValidator{

    public static final String NO_INCOMING_FLOW_PROBLEM = "Intermediate Flow node has no incoming flow";
    public static final String NO_INCOMING_FLOW_PROBLEM_DESCRIPTION = "Intermediate Flow node has to have an incoming flow";
    public static final String NO_OUTGOING_FLOW_PROBLEM = "Intermediate Flow node has no outgoing flow";
    public static final String NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION = "Intermediate Flow node has to have an outgoing flow";
    public static final String INCOMING_FLOW_ON_START_EVENT_PROBLEM = "Intermediate Flow node Start event should not have incoming flow";
    public static final String INCOMING_FLOW_ON_START_EVENT_PROBLEM_DESCRIPTION = "Intermediate Flow node Start event has to have an empty incoming flow";
    public static final String OUTGOING_FLOW_ON_END_EVENT_PROBLEM = "Intermediate Flow node End event should not have outgoing flow";
    public static final String OUTGOING_FLOW_ON_END_EVENT_PROBLEM_DESCRIPTION = "Intermediate Flow node End event should not have outgoing flow";
    public static final String INTERMEDIATE_FLOW_VALIDATOR_NAME = "BPMN Intermediate Flow node validator";

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext) {
        List<ModelValidationError> errors = new ArrayList<>();
        getFlowElements(bpmnModel,
            FlowNode.class).forEach(flowNode -> {
            errors.addAll(validateTaskFlow(flowNode));
        });

        return errors.stream();
    }

    private List<ModelValidationError> validateTaskFlow(FlowNode flowNode) {

        List<ModelValidationError> errors = new ArrayList<>();
        if (flowNode instanceof StartEvent) {
            if (CollectionUtils.isEmpty(flowNode.getOutgoingFlows())) {
                errors.add(createModelValidationError(NO_OUTGOING_FLOW_PROBLEM,
                    NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION,
                    INTERMEDIATE_FLOW_VALIDATOR_NAME));
            }
            if (CollectionUtils.isNotEmpty(flowNode.getIncomingFlows())) {
                errors.add(createModelValidationError(INCOMING_FLOW_ON_START_EVENT_PROBLEM,
                    INCOMING_FLOW_ON_START_EVENT_PROBLEM_DESCRIPTION,
                    INTERMEDIATE_FLOW_VALIDATOR_NAME));
            }

        } else if (flowNode instanceof EndEvent) {
            if (CollectionUtils.isEmpty(flowNode.getIncomingFlows())) {
                errors.add(createModelValidationError(NO_INCOMING_FLOW_PROBLEM,
                    NO_INCOMING_FLOW_PROBLEM_DESCRIPTION,
                    INTERMEDIATE_FLOW_VALIDATOR_NAME));
            }
            if (CollectionUtils.isNotEmpty(flowNode.getOutgoingFlows())) {
                errors.add(createModelValidationError(OUTGOING_FLOW_ON_END_EVENT_PROBLEM,
                    OUTGOING_FLOW_ON_END_EVENT_PROBLEM_DESCRIPTION,
                    INTERMEDIATE_FLOW_VALIDATOR_NAME));
            }

        } else {
            if (CollectionUtils.isEmpty(flowNode.getIncomingFlows())) {
                errors.add(createModelValidationError(NO_INCOMING_FLOW_PROBLEM,
                    NO_INCOMING_FLOW_PROBLEM_DESCRIPTION,
                    INTERMEDIATE_FLOW_VALIDATOR_NAME));
            }

            if (CollectionUtils.isEmpty(flowNode.getOutgoingFlows())) {
                errors.add(createModelValidationError(NO_OUTGOING_FLOW_PROBLEM,
                    NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION,
                    INTERMEDIATE_FLOW_VALIDATOR_NAME));
            }
        }
        return errors;
    }
}
