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

import java.util.ArrayList;
import java.util.List;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.apache.commons.collections.CollectionUtils;

public class StartEventIncomingOutgoingFlowValidator implements FlowNodeFlowsValidator {

    public static final String NO_OUTGOING_FLOW_PROBLEM = "Start event has no outgoing flow";
    public static final String NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION =
        "Start event [name: '%s', id: '%s'] has to have an outgoing flow";
    public static final String INCOMING_FLOW_ON_START_EVENT_PROBLEM = "Start event should not have incoming flow";
    public static final String INCOMING_FLOW_ON_START_EVENT_PROBLEM_DESCRIPTION =
        "Start event [name: '%s', id: '%s'] should not have incoming flow";
    public static final String START_EVENT_FLOWS_VALIDATOR_NAME = "BPMN Start event validator";

    @Override
    public List<ModelValidationError> validate(FlowNode flowNode) {
        List<ModelValidationError> errors = new ArrayList<>();

        if (CollectionUtils.isEmpty(flowNode.getOutgoingFlows())) {
            errors.add(
                createModelValidationError(
                    NO_OUTGOING_FLOW_PROBLEM,
                    format(NO_OUTGOING_FLOW_PROBLEM_DESCRIPTION, flowNode.getName(), flowNode.getId()),
                    START_EVENT_FLOWS_VALIDATOR_NAME,
                    null,
                    flowNode.getId()
                )
            );
        }
        if (CollectionUtils.isNotEmpty(flowNode.getIncomingFlows())) {
            errors.add(
                createModelValidationError(
                    INCOMING_FLOW_ON_START_EVENT_PROBLEM,
                    format(INCOMING_FLOW_ON_START_EVENT_PROBLEM_DESCRIPTION, flowNode.getName(), flowNode.getId()),
                    START_EVENT_FLOWS_VALIDATOR_NAME,
                    null,
                    flowNode.getId()
                )
            );
        }
        return errors;
    }

    @Override
    public boolean canValidate(FlowNode flowNode) {
        return flowNode instanceof StartEvent;
    }
}
