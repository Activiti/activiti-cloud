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
class ModelNameValidatorImplIT {

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
