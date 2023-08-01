package org.activiti.cloud.services.modeling.validation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.activiti.cloud.services.modeling.validation.ModelValidatorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ModelValidatorConfiguration.class)
class ModelValidatorImplIT {

    @Autowired
    private ModelNameValidator modelNameValidator;

    @Test
    void should_validateWithErrorForModelNameRequired_when_validateWithNullName() {
        Model model = new ModelImpl();
        model.setName(null);

        executeValidateMethodTest(model, "The model name is required");
    }

    @Test
    void should_validateWithOneError_when_validateWithInvalidDNSName() {
        Model model = new ModelImpl();
        model.setName("model-error-");

        executeValidateMethodTest(
            model,
            "The model name should follow DNS-1035 conventions:" +
            " it must consist of lower case alphanumeric characters or '-'," +
            " and must start and end with an alphanumeric character: 'model-error-'"
        );
    }

    private void executeValidateMethodTest(Model model, String expected) {
        Stream<ModelValidationError> validationResult = this.modelNameValidator.validate(model);

        List<ModelValidationError> errors = validationResult.collect(Collectors.toList());

        assertEquals(1, errors.size());
        assertEquals(expected, errors.get(0).getDescription());
    }
}
