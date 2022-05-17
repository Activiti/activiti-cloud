package org.activiti.cloud.services.modeling.validation.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.util.List;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.config.ModelingApiAutoConfiguration;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.services.common.util.FileUtils;
import org.activiti.cloud.services.modeling.TestConfiguration;
import org.activiti.cloud.services.modeling.converter.ProcessModelConverterConfiguration;
import org.activiti.cloud.services.modeling.service.JsonConverterConfiguration;
import org.activiti.cloud.services.modeling.validation.JsonSchemaModelValidatorConfiguration;
import org.activiti.cloud.services.modeling.validation.ProcessModelValidatorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
    JsonSchemaModelValidatorConfiguration.class,
    ProcessModelValidatorConfiguration.class,
    ModelingApiAutoConfiguration.class,
    ProcessModelConverterConfiguration.class,
    JsonConverterConfiguration.class,
    TestConfiguration.class})
public class ProcessExtensionsValidatorTest {

    @Autowired
    private ProcessExtensionsModelValidator processExtensionsValidator;

    @Test
    public void shouldNotBeValidWhenDisplayIsTrueAndDiplayNameIsAbsent() throws IOException {
        byte[] fileContent = FileUtils.resourceAsByteArray("extensions/process-with-displayable-variable-without-name.json");

        SemanticModelValidationException semanticModelValidationException = catchThrowableOfType(
            () -> processExtensionsValidator.validateModelExtensions(fileContent, ValidationContext.EMPTY_CONTEXT),
            SemanticModelValidationException.class);

        List<ModelValidationError> validationErrors = semanticModelValidationException.getValidationErrors();
        assertThat(validationErrors).hasSize(2);
        assertThat(validationErrors).
            extracting("problem")
            .containsOnly("subject must not be valid against schema {\"required\":[\"display\"],\"properties\":{\"display\":{\"const\":true}}}",
                          "required key [displayName] not found");
    }

    @Test
    public void shouldBeValidWhenDisplayIsTrueAndDiplayNameIsPresent() throws IOException {
        byte[] fileContent = FileUtils.resourceAsByteArray("extensions/process-with-displayable-variable-with-name.json");
        processExtensionsValidator.validateModelExtensions(fileContent, ValidationContext.EMPTY_CONTEXT);
    }

    @Test
    public void shouldBeValidWhenDisplayIsFalse() throws IOException {
        byte[] fileContent = FileUtils.resourceAsByteArray("extensions/process-with-display-process-variable-false.json");
        processExtensionsValidator.validateModelExtensions(fileContent, ValidationContext.EMPTY_CONTEXT);
    }

    @Test
    public void shouldBeValidABasicProcess() throws IOException {
        byte[] fileContent = FileUtils.resourceAsByteArray("extensions/basic-process.json");
        processExtensionsValidator.validateModelExtensions(fileContent, ValidationContext.EMPTY_CONTEXT);
    }

}
