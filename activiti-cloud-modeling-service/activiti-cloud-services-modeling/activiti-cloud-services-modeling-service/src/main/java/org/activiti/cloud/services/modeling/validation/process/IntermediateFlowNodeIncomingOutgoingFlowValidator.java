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

import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.EventSubProcess;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class IntermediateFlowNodeIncomingOutgoingFlowValidator implements FlowNodeFlowsValidator {

    public static final String NO_INCOMING_FLOW_PROBLEM = "Intermediate Flow node has no incoming flow";
    public static final String NO_INCOMING_FLOW_PROBLEM_DESCRIPTION = "Intermediate Flow node has to have an incoming flow";
    public static final String NO_OUTGOING_FLOW_PROBLEM = "Intermediate Flow node has no outgoing flow";
    public static final String NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION = "Intermediate Flow node has to have an outgoing flow";
    public static final String INTERMEDIATE_FLOWS_VALIDATOR_NAME = "BPMN Intermediate Flow node validator";

    @Override
    public List<ModelValidationError> validate(FlowNode flowNode) {
        List<ModelValidationError> errors = new ArrayList<>();

        if (CollectionUtils.isEmpty(flowNode.getIncomingFlows())) {
            errors.add(createModelValidationError(NO_INCOMING_FLOW_PROBLEM,
                NO_INCOMING_FLOW_PROBLEM_DESCRIPTION,
                INTERMEDIATE_FLOWS_VALIDATOR_NAME));
        }

        if (CollectionUtils.isEmpty(flowNode.getOutgoingFlows())) {
            errors.add(createModelValidationError(NO_OUTGOING_FLOW_PROBLEM,
                NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION,
                INTERMEDIATE_FLOWS_VALIDATOR_NAME));
        }
        return errors;
    }

    @Override
    public boolean canValidate(FlowNode flowNode) {
        return !(flowNode instanceof StartEvent) &&
            !(flowNode instanceof EndEvent) &&
            !(flowNode instanceof EventSubProcess);
    }
}
