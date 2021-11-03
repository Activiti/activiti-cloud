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
import static org.activiti.cloud.services.modeling.validation.process.CreateBpmnModelTestHelper.createOneServiceTaskTestProcess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContentConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BpmnModelServiceTaskImplementationValidatorTest {

    private BpmnModelServiceTaskImplementationValidator validator;
    private final String theServiceTaskId = "theServiceTask";

    @Mock
    private ValidationContext validationContext;
    @Mock
    private ConnectorModelType connectorModelType;
    @Mock
    private ConnectorModelContentConverter connectorModelContentConverter;

    @BeforeEach
    void setUp() {
        validator = new BpmnModelServiceTaskImplementationValidator(connectorModelType, connectorModelContentConverter);
    }

    @Test
    public void should_returnError_when_validatingInvalidImplementation() {
        String invalidServiceTaskId = "invalidServiceTaskId";
        BpmnModel model = createOneServiceTaskTestProcess("invalid-implementation");
        model.getMainProcess().getFlowElement(theServiceTaskId).setId(invalidServiceTaskId);

        assertThat(validator.validate(model, validationContext))
                .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName,
                        ModelValidationError::getReferenceId)
                .contains(tuple(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_PROBLEM,
                        format(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, invalidServiceTaskId),
                        BpmnModelServiceTaskImplementationValidator.SERVICE_USER_TASK_VALIDATOR_NAME, null));
    }

    @Test
    public void should_returnError_when_validatingIncompleteServiceImplementation() {
        String incompleteServiceTaskId = "incompleteServiceTaskId";
        BpmnModel model = createOneServiceTaskTestProcess("email-service");
        model.getMainProcess().getFlowElement(theServiceTaskId).setId(incompleteServiceTaskId);

        assertThat(validator.validate(model, validationContext))
                .extracting(ModelValidationError::getProblem,
                        ModelValidationError::getDescription,
                        ModelValidationError::getValidatorSetName,
                        ModelValidationError::getReferenceId)
                .contains(tuple(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_PROBLEM,
                        format(BpmnModelServiceTaskImplementationValidator.INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, incompleteServiceTaskId),
                        BpmnModelServiceTaskImplementationValidator.SERVICE_USER_TASK_VALIDATOR_NAME, null));
    }

    @Test
    public void should_returnEmpty_when_validatingScriptImplementation() {
        String contentServiceTaskId = "contentServiceTaskId";
        BpmnModel model = createOneServiceTaskTestProcess("script.EXECUTE");
        model.getMainProcess().getFlowElement(theServiceTaskId).setId(contentServiceTaskId);

        assertThat(validator.validate(model, validationContext).count()).isEqualTo(0);
    }

    @Test
    public void should_returnEmpty_when_validatingEmailServiceImplementation() {
        String contentServiceTaskId = "contentServiceTaskId";
        BpmnModel model = createOneServiceTaskTestProcess("email-service.SEND");
        model.getMainProcess().getFlowElement(theServiceTaskId).setId(contentServiceTaskId);

        assertThat(validator.validate(model, validationContext).count()).isEqualTo(0);
    }

    @Test
    public void should_returnEmpty_when_validatingDocgenServiceImplementation() {
        String contentServiceTaskId = "contentServiceTaskId";
        BpmnModel model = createOneServiceTaskTestProcess("docgen-service.GENERATE");
        model.getMainProcess().getFlowElement(theServiceTaskId).setId(contentServiceTaskId);

        assertThat(validator.validate(model, validationContext).count()).isEqualTo(0);
    }

    @Test
    public void should_returnEmpty_when_validatingContentServiceImplementation() {
        String contentServiceTaskId = "contentServiceTaskId";
        BpmnModel model = createOneServiceTaskTestProcess("content-service.MOVE_FOLDER");
        model.getMainProcess().getFlowElement(theServiceTaskId).setId(contentServiceTaskId);

        assertThat(validator.validate(model, validationContext).count()).isEqualTo(0);
    }

}