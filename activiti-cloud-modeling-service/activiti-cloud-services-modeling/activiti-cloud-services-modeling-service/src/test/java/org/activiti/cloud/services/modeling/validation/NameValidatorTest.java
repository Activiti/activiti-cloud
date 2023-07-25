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
package org.activiti.cloud.services.modeling.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.activiti.cloud.modeling.api.ModelValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NameValidatorTest {

    private NameValidator nameValidator;

    @BeforeEach
    public void setup() {
        nameValidator = new NameValidator() {};
    }

    @Test
    public void should_returnEmptyStream_when_theInputIsValid() {
        Stream<ModelValidationError> errors = nameValidator.validateName("This is @ test with spaces", "myType");
        assertThat(errors).isEmpty();
    }

    @Test
    public void should_returnFieldRequiredError_when_itIsNull() {
        Stream<ModelValidationError> errors = nameValidator.validateName(null, "myType");
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly("field.required", "The myType name is required");
    }

    @Test
    public void should_returnFieldEmptyError_when_itIsAnEmptyString() {
        Stream<ModelValidationError> errors = nameValidator.validateName("", "myType");
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly("field.empty", "The myType name cannot be empty");
    }

    @Test
    public void should_returnFieldEmptyError_when_itContainsOnlyBlankSpaces() {
        Stream<ModelValidationError> errors = nameValidator.validateName("   ", "myType");
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly("field.empty", "The myType name cannot be empty");
    }

    @Test
    public void should_returnLengthGreaterError_when_textIsTooLong() {
        Stream<ModelValidationError> errors = nameValidator.validateName(
            "Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 g",
            "myType"
        );
        assertThat(errors)
            .flatExtracting(ModelValidationError::getErrorCode, ModelValidationError::getDescription)
            .containsOnly(
                "length.greater",
                "The myType name length cannot be greater than 100: 'Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 ghi 789 jkl Abc 123 def 456 g'"
            );
    }
}
