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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;

/**
 * Implementation of {@link BpmnCommonModelValidator} for validating Incoming and Outgoing flows
 */
public class BpmnModelIncomingOutgoingFlowValidator implements BpmnCommonModelValidator {

    private final List<FlowNodeFlowsValidator> flowNodeFlowsValidators;
    private final FlowElementsExtractor flowElementsExtractor;

    public BpmnModelIncomingOutgoingFlowValidator(
        List<FlowNodeFlowsValidator> flowNodeFlowsValidators,
        FlowElementsExtractor flowElementsExtractor
    ) {
        this.flowNodeFlowsValidators = flowNodeFlowsValidators;
        this.flowElementsExtractor = flowElementsExtractor;
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext) {
        List<ModelValidationError> errors = new ArrayList<>();
        flowElementsExtractor
            .extractFlowElements(bpmnModel, FlowNode.class)
            .forEach(flowNode -> errors.addAll(validateFlowNode(flowNode)));
        return errors.stream();
    }

    private List<ModelValidationError> validateFlowNode(FlowNode flowNode) {
        List<ModelValidationError> errors = new ArrayList<>();

        for (FlowNodeFlowsValidator validator : flowNodeFlowsValidators) {
            if (validator.canValidate(flowNode)) {
                errors.addAll(validator.validate(flowNode));
            }
        }
        return errors;
    }
}
