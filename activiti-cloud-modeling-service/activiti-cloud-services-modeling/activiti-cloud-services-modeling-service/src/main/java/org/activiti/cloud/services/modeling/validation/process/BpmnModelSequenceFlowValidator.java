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
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link BpmnCommonModelValidator} for validating Sequence flow when empty source or target references are provided
 */
public class BpmnModelSequenceFlowValidator implements BpmnCommonModelValidator {

    public static final String NO_SOURCE_REF_PROBLEM = "Sequence flow has no source reference";
    public static final String NO_SOURCE_REF_PROBLEM_DESCRIPTION =
        "Sequence flow [name: '%s', id: '%s'] has to have a source reference";
    public static final String NO_TARGET_REF_PROBLEM = "Sequence flow has no target reference";
    public static final String NO_TARGET_REF_PROBLEM_DESCRIPTION =
        "Sequence flow [name: '%s', id: '%s'] has to have a target reference";
    public static final String SEQUENCE_FLOW_VALIDATOR_NAME = "BPMN sequence flow validator";

    private final FlowElementsExtractor flowElementsExtractor;

    public BpmnModelSequenceFlowValidator(FlowElementsExtractor flowElementsExtractor) {
        this.flowElementsExtractor = flowElementsExtractor;
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext) {
        List<ModelValidationError> errors = new ArrayList<>();
        flowElementsExtractor
            .extractFlowElements(bpmnModel, SequenceFlow.class)
            .forEach(sequenceFlow -> errors.addAll(validateSequenceFlow(sequenceFlow)));
        return errors.stream();
    }

    private List<ModelValidationError> validateSequenceFlow(SequenceFlow sequenceFlow) {
        List<ModelValidationError> errors = new ArrayList<>();
        if (StringUtils.isEmpty(sequenceFlow.getSourceRef())) {
            errors.add(
                createModelValidationError(
                    NO_SOURCE_REF_PROBLEM,
                    format(NO_SOURCE_REF_PROBLEM_DESCRIPTION, sequenceFlow.getName(), sequenceFlow.getId()),
                    SEQUENCE_FLOW_VALIDATOR_NAME,
                    null,
                    sequenceFlow.getId()
                )
            );
        }

        if (StringUtils.isEmpty(sequenceFlow.getTargetRef())) {
            errors.add(
                createModelValidationError(
                    NO_TARGET_REF_PROBLEM,
                    format(NO_TARGET_REF_PROBLEM_DESCRIPTION, sequenceFlow.getName(), sequenceFlow.getId()),
                    SEQUENCE_FLOW_VALIDATOR_NAME,
                    null,
                    sequenceFlow.getId()
                )
            );
        }

        return errors;
    }
}
