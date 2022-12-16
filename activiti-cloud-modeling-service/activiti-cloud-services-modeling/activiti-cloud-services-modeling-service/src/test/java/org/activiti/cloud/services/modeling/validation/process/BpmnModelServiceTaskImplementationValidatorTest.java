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

import java.util.UUID;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BpmnModelServiceTaskImplementationValidatorTest {

    @InjectMocks
    private BpmnModelServiceTaskImplementationValidator validator;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private FlowElementsExtractor flowElementsExtractor;

    @Test
    public void should_returnError_when_validatingInvalidImplementation() {
        //given
        ServiceTask serviceTask = buildServiceTask("invalid-implementation");
        BpmnModel model = new BpmnModel();

        //when
        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //then
        assertThat(validator.validate(model, validationContext))
                .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName,
                        ModelValidationError::getReferenceId)
                .contains(tuple(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_PROBLEM,
                        format(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, serviceTask.getId()),
                        BpmnModelServiceTaskImplementationValidator.SERVICE_USER_TASK_VALIDATOR_NAME, null));
    }

    private ServiceTask buildServiceTask(String implementation) {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(implementation);
        serviceTask.setName("The Service Task");
        serviceTask.setId(UUID.randomUUID().toString());
        return serviceTask;
    }

    @Test
    public void should_returnError_when_validatingIncompleteServiceImplementation() {
        //given
        ServiceTask serviceTask = buildServiceTask("invalid-implementation");
        BpmnModel model = new BpmnModel();

        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //when
        final Stream<ModelValidationError> validationResult = validator.validate(model, validationContext);

        //then
        assertThat(validationResult)
                .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName,
                        ModelValidationError::getReferenceId)
                .contains(tuple(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_PROBLEM,
                        format(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, serviceTask.getId()),
                        BpmnModelServiceTaskImplementationValidator.SERVICE_USER_TASK_VALIDATOR_NAME, null));
    }


    private static Stream<Arguments> provideValidServiceTaskImplementations() {
        return Stream.of(
            Arguments.of("script.EXECUTE"),
            Arguments.of("email-service.SEND"),
            Arguments.of("docgen-service.GENERATE"),
            Arguments.of("content-service.MOVE_FOLDER"),
            Arguments.of("hxp-content-service.SET_PERMISSION")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidServiceTaskImplementations")
    public void should_returnEmpty_when_validImplementation(String implementation) {
        //given
        ServiceTask serviceTask = buildServiceTask(implementation);
        BpmnModel model = new BpmnModel();

        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //when
        final Stream<ModelValidationError> validationResult = validator.validate(model, validationContext);

        //then
        assertThat(validationResult).isEmpty();
    }

}
