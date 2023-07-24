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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BpmnModelNameValidatorTest {

    private ValidationContext validationContext = null;

    private BpmnModelNameValidator bpmnModelNameValidator;

    @Mock
    private BpmnModel bpmnModel;

    @Mock
    private Process process;

    @BeforeEach
    public void setup() {
        bpmnModelNameValidator = new BpmnModelNameValidator();
        when(bpmnModel.getMainProcess()).thenReturn(process);
    }

    @Test
    public void should_returnEmptyStream_when_theInputIsValid() {
        when(process.getName()).thenReturn("This is a test!");
        Stream<ModelValidationError> errors = bpmnModelNameValidator.validate(bpmnModel, validationContext);
        assertThat(errors).isEmpty();
    }

    @Test
    public void should_returnFieldRequiredError_when_itIsNull() {
        when(process.getName()).thenReturn(null);
        Stream<ModelValidationError> errors = bpmnModelNameValidator.validate(bpmnModel, validationContext);
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly("field.required", "The process name is required");
    }

    @Test
    public void should_returnFieldEmptyError_when_itIsAnEmptyString() {
        when(process.getName()).thenReturn("");
        Stream<ModelValidationError> errors = bpmnModelNameValidator.validate(bpmnModel, validationContext);
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly("field.empty", "The process name cannot be empty");
    }

    @Test
    public void should_returnFieldEmptyError_when_itContainsOnlyBlankSpaces() {
        when(process.getName()).thenReturn("   ");
        Stream<ModelValidationError> errors = bpmnModelNameValidator.validate(bpmnModel, validationContext);
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly("field.empty", "The process name cannot be empty");
    }

    @Test
    public void should_returnLengthGreaterError_when_textIsTooLong() {
        when(process.getName())
            .thenReturn(
                "Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 g"
            );
        Stream<ModelValidationError> errors = bpmnModelNameValidator.validate(bpmnModel, validationContext);
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly(
                "length.greater",
                "The process name length cannot be greater than 100: 'Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 g'"
            );
    }
}
