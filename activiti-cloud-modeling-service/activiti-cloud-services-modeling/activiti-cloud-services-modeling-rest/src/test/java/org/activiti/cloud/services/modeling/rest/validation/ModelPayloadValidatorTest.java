package org.activiti.cloud.services.modeling.rest.validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.cloud.modeling.api.Model;
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
        modelPayloadValidator = new ModelPayloadValidator(true);
    }

    @Test
    public void should_returnEmptyStream_when_theInputIsValid() {
        when(model.getName()).thenReturn("This is a test!");

        modelPayloadValidator.validatePayload(model, this.errors);

        verify(errors, never()).rejectValue(any(), any(), any(), any());
    }

    @Test
    public void should_returnFieldRequiredError_when_itIsNull() {
        when(model.getName()).thenReturn(null);

        modelPayloadValidator.validatePayload(model, this.errors);

        verify(errors).rejectValue("name", "field.required", "The model name is required");
    }

    @Test
    public void should_returnFieldEmptyError_when_itIsAnEmptyString() {
        when(model.getName()).thenReturn("");
        modelPayloadValidator.validatePayload(model, this.errors);
        verify(errors).rejectValue("name", "field.empty", "The model name cannot be empty");
    }

    @Test
    public void should_returnFieldEmptyError_when_itContainsOnlyBlankSpaces() {
        when(model.getName()).thenReturn("   ");
        modelPayloadValidator.validatePayload(model, this.errors);
        verify(errors).rejectValue("name", "field.empty", "The model name cannot be empty");
    }

    @Test
    public void should_returnLengthGreaterError_when_textIsTooLong() {
        when(model.getName()).thenReturn("Abc 123 def 456 ghi 789 jkl");
        modelPayloadValidator.validatePayload(model, this.errors);
        verify(errors).rejectValue("name", "length.greater", "The model name length cannot be greater than 26: 'Abc 123 def 456 ghi 789 jkl'");
    }


}
