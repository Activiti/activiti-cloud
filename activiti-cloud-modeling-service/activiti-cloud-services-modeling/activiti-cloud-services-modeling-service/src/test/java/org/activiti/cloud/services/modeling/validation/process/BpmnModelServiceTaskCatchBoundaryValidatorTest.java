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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ErrorEventDefinition;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BpmnModelServiceTaskCatchBoundaryValidatorTest {

    private BpmnModelServiceTaskCatchBoundaryValidator validator;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private FlowElementsExtractor flowElementsExtractor;

    @BeforeEach
    public void setUp() {
        validator = new BpmnModelServiceTaskCatchBoundaryValidator(flowElementsExtractor, Arrays.asList(ServiceTaskImplementationType.SCRIPT_TASK));
    }

    @Test
    public void should_returnError_when_taskIsScriptWithNoErrorBoundaryEvent() {
        //given
        ServiceTask serviceTask = buildServiceTask("script.EXECUTE", Arrays.asList(buildSignalBoundaryEvent()));
        BpmnModel model = new BpmnModel();

        //when
        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //then
        assertThat(validator.validate(model, validationContext))
            .extracting(ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::isWarning)
            .contains(tuple(BpmnModelServiceTaskCatchBoundaryValidator.MISSING_BOUNDARY_WARNING,
                format(BpmnModelServiceTaskCatchBoundaryValidator.INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, serviceTask.getId()),
                BpmnModelServiceTaskCatchBoundaryValidator.SERVICE_TASK_VALIDATOR_NAME, true));
    }


    @Test
    public void should_returnError_when_scriptHasNoBoundaryEvent() {
        //given
        ServiceTask serviceTask = buildServiceTask("script.EXECUTE", Collections.emptyList());
        BpmnModel model = new BpmnModel();

        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //when
        final Stream<ModelValidationError> validationResult = validator.validate(model, validationContext);

        //then
        assertThat(validator.validate(model, validationContext))
            .extracting(ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::isWarning)
            .contains(tuple(BpmnModelServiceTaskCatchBoundaryValidator.MISSING_BOUNDARY_WARNING,
                format(BpmnModelServiceTaskCatchBoundaryValidator.INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, serviceTask.getId()),
                BpmnModelServiceTaskCatchBoundaryValidator.SERVICE_TASK_VALIDATOR_NAME, true));
    }

    @Test
    public void should_returnEmpty_when_scriptHasErrorBoundary() {
        //given
        ServiceTask serviceTask = buildServiceTask("script.EXECUTE", Arrays.asList(buildErrorBoundaryEvent()));
        BpmnModel model = new BpmnModel();

        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //when
        final Stream<ModelValidationError> validationResult = validator.validate(model, validationContext);

        //then
        assertThat(validationResult).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideValidServiceTaskImplementations")
    public void should_returnEmpty_when_isNotScript(String implementation) {
        //given
        ServiceTask serviceTask = buildServiceTask(implementation, Collections.emptyList());
        BpmnModel model = new BpmnModel();

        given(flowElementsExtractor.extractFlowElements(model, ServiceTask.class))
            .willReturn(singleton(serviceTask));

        //when
        final Stream<ModelValidationError> validationResult = validator.validate(model, validationContext);

        //then
        assertThat(validationResult).isEmpty();
    }

    private static Stream<Arguments> provideValidServiceTaskImplementations() {
        return Stream.of(
            Arguments.of("email-service.SEND"),
            Arguments.of("docgen-service.GENERATE"),
            Arguments.of("content-service.MOVE_FOLDER"),
            Arguments.of("hxp-content-service.SET_PERMISSION")
        );
    }

    private ServiceTask buildServiceTask(String implementation, List<BoundaryEvent> boundaryEvents) {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(implementation);
        serviceTask.setName("The Service Task");
        serviceTask.setId(UUID.randomUUID().toString());
        serviceTask.setBoundaryEvents(boundaryEvents);
        return serviceTask;
    }

    private BoundaryEvent buildSignalBoundaryEvent() {
        BoundaryEvent boundaryEvent = new BoundaryEvent();
        boundaryEvent.setEventDefinitions(Arrays.asList(new SignalEventDefinition()));
        return boundaryEvent;
    }

    private BoundaryEvent buildErrorBoundaryEvent() {
        BoundaryEvent boundaryEvent = new BoundaryEvent();
        boundaryEvent.setEventDefinitions(Arrays.asList(new ErrorEventDefinition()));
        return boundaryEvent;
    }
}
