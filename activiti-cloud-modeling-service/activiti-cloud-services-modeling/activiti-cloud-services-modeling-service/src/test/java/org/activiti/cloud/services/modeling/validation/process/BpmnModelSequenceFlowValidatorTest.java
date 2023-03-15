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
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BpmnModelSequenceFlowValidatorTest {

    private static final String TEST_SEQUENCE_ID = "testSequenceId";
    private static final String TEST_SEQUENCE_NAME = "testSequenceName";

    @InjectMocks
    private BpmnModelSequenceFlowValidator bpmnModelSequenceFlowValidator;

    @Mock
    private FlowElementsExtractor flowElementsExtractor;

    @Mock
    private ValidationContext validationContext;

    @Test
    public void should_returnError_when_noSourceReferenceIsSpecified() {
        //given
        SequenceFlow sequenceFlow = new SequenceFlow(null, "theTask");
        sequenceFlow.setId(TEST_SEQUENCE_ID);
        sequenceFlow.setName(TEST_SEQUENCE_NAME);

        BpmnModel bpmnModel = new BpmnModel();
        given(flowElementsExtractor.extractFlowElements(bpmnModel, SequenceFlow.class))
            .willReturn(singleton(sequenceFlow));

        //when
        final Stream<ModelValidationError> validationResult = bpmnModelSequenceFlowValidator.validate(
            bpmnModel,
            validationContext
        );

        //then
        assertThat(validationResult)
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    BpmnModelSequenceFlowValidator.NO_SOURCE_REF_PROBLEM,
                    format(
                        BpmnModelSequenceFlowValidator.NO_SOURCE_REF_PROBLEM_DESCRIPTION,
                        TEST_SEQUENCE_NAME,
                        TEST_SEQUENCE_ID
                    ),
                    BpmnModelSequenceFlowValidator.SEQUENCE_FLOW_VALIDATOR_NAME,
                    TEST_SEQUENCE_ID
                )
            );
    }

    @Test
    public void should_returnError_when_noTargetReferenceIsSpecified() {
        //given
        SequenceFlow sequenceFlow = new SequenceFlow("start", null);
        sequenceFlow.setId(TEST_SEQUENCE_ID);
        sequenceFlow.setName(TEST_SEQUENCE_NAME);

        BpmnModel bpmnModel = new BpmnModel();
        given(flowElementsExtractor.extractFlowElements(bpmnModel, SequenceFlow.class))
            .willReturn(singleton(sequenceFlow));

        //when
        final Stream<ModelValidationError> validationResult = bpmnModelSequenceFlowValidator.validate(
            bpmnModel,
            validationContext
        );

        //then
        assertThat(validationResult)
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    BpmnModelSequenceFlowValidator.NO_TARGET_REF_PROBLEM,
                    format(
                        BpmnModelSequenceFlowValidator.NO_TARGET_REF_PROBLEM_DESCRIPTION,
                        TEST_SEQUENCE_NAME,
                        TEST_SEQUENCE_ID
                    ),
                    BpmnModelSequenceFlowValidator.SEQUENCE_FLOW_VALIDATOR_NAME,
                    TEST_SEQUENCE_ID
                )
            );
    }
}
