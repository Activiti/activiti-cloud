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

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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

    @Test
    public void shouldBeValidWhenAnalyticsIsPresent() throws IOException {
        byte[] fileContent = FileUtils.resourceAsByteArray("extensions/process-with-analytics-variable.json");
        processExtensionsValidator.validateModelExtensions(fileContent, ValidationContext.EMPTY_CONTEXT);
    }

    @Test
    public void shouldBeInvalidWhenAnalyticsIsPresent() throws IOException {
        byte[] fileContent = FileUtils.resourceAsByteArray("extensions/process-with-invalid-analytics-variable.json");

        SemanticModelValidationException semanticModelValidationException = catchThrowableOfType(
            () -> processExtensionsValidator.validateModelExtensions(fileContent, ValidationContext.EMPTY_CONTEXT),
            SemanticModelValidationException.class);

        List<ModelValidationError> validationErrors = semanticModelValidationException.getValidationErrors();
        assertThat(validationErrors).hasSize(3);
        assertThat(validationErrors).
            extracting("problem")
            .containsOnly("string [file] does not match pattern integer|string|boolean|date|datetime",
                          "string [folder] does not match pattern integer|string|boolean|date|datetime",
                          "string [json] does not match pattern integer|string|boolean|date|datetime");
    }
}
