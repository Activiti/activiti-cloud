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
package org.activiti.cloud.services.modeling.validation.extensions;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.ServiceTaskActionType;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContentConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskMappingsServiceTaskImplementationValidatorTest {

    private TaskMappingsServiceTaskImplementationValidator validator;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ConnectorModelType connectorModelType;

    @Mock
    private ConnectorModelContentConverter connectorModelContentConverter;

    @Mock
    private MappingModel mappingModel;

    private final String PROCESS_ID = "processId";
    private final String CUSTOM_ACTION = "custom-connector.ACTION";
    private final String INPUTS_TEXT = "INPUTS";
    private final String ID_TEXT = "ID";

    @BeforeEach
    void setUp() {
        validator =
            new TaskMappingsServiceTaskImplementationValidator(connectorModelType, connectorModelContentConverter);
    }

    @Test
    public void should_returnEmpty_when_validatingTaskMappingsOnServiceImplementation() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation("content-service.");

        when(mappingModel.getFlowNode()).thenReturn(serviceTask);

        assertThat(validator.validateTaskMappings(List.of(mappingModel), null, validationContext).count()).isEqualTo(0);
    }

    @Test
    public void should_returnError_when_validatingTaskMappingsInvalidConnectorAction() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId(ID_TEXT);
        serviceTask.setImplementation(CUSTOM_ACTION);
        ServiceTaskActionType actionType = ServiceTaskActionType.fromValue(INPUTS_TEXT);

        when(mappingModel.getProcessId()).thenReturn(PROCESS_ID);
        when(mappingModel.getFlowNode()).thenReturn(serviceTask);
        when(mappingModel.getAction()).thenReturn(actionType);

        assertThat(validator.validateTaskMappings(List.of(mappingModel), null, validationContext))
            .extracting(
                ModelValidationError::getProblem,
                ModelValidationError::getDescription,
                ModelValidationError::getValidatorSetName,
                ModelValidationError::getReferenceId
            )
            .contains(
                tuple(
                    format(
                        TaskMappingsServiceTaskImplementationValidator.UNKNOWN_CONNECTOR_ACTION_VALIDATION_ERROR_PROBLEM,
                        INPUTS_TEXT,
                        ID_TEXT,
                        CUSTOM_ACTION
                    ),
                    format(
                        TaskMappingsServiceTaskImplementationValidator.UNKNOWN_CONNECTOR_ACTION_VALIDATION_ERROR_DESCRIPTION,
                        PROCESS_ID,
                        INPUTS_TEXT,
                        ID_TEXT,
                        CUSTOM_ACTION
                    ),
                    null,
                    null
                )
            );
    }
}
