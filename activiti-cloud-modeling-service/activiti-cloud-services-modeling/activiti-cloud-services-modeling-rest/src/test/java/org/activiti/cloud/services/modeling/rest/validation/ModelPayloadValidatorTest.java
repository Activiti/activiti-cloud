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
package org.activiti.cloud.services.modeling.rest.validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.services.modeling.validation.model.ModelNameValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class ModelPayloadValidatorTest {

    private ModelPayloadValidator modelPayloadValidator;

    @Mock
    private Errors errors;

    @Mock
    private Model model;

    @BeforeEach
    public void setup() {
        modelPayloadValidator = new ModelPayloadValidator(true, new ModelNameValidator());
    }

    @Test
    public void should_returnEmptyStream_when_theInputIsValid() {
        when(model.getDisplayName()).thenReturn("This is a test!");

        modelPayloadValidator.validatePayload(model, this.errors);

        verify(errors, never()).rejectValue(any(), any(), any(), any());
    }

    @Test
    public void should_returnFieldRequiredError_when_itIsNull() {
        when(model.getDisplayName()).thenReturn(null);

        modelPayloadValidator.validatePayload(model, this.errors);

        verify(errors).rejectValue("name", "field.required", "The model name is required");
    }

    @Test
    public void should_returnFieldEmptyError_when_itIsAnEmptyString() {
        when(model.getDisplayName()).thenReturn("");
        modelPayloadValidator.validatePayload(model, this.errors);
        verify(errors).rejectValue("name", "field.empty", "The model name cannot be empty");
    }

    @Test
    public void should_returnFieldEmptyError_when_itContainsOnlyBlankSpaces() {
        when(model.getDisplayName()).thenReturn("   ");
        modelPayloadValidator.validatePayload(model, this.errors);
        verify(errors).rejectValue("name", "field.empty", "The model name cannot be empty");
    }

    @Test
    public void should_returnLengthGreaterError_when_textIsTooLong() {
        String name = "a".repeat(101);
        when(model.getDisplayName()).thenReturn(name);
        modelPayloadValidator.validatePayload(model, this.errors);
        verify(errors)
            .rejectValue(
                "name",
                "length.greater",
                String.format("The model name length cannot be greater than 100: '%s'", name)
            );
    }
}
