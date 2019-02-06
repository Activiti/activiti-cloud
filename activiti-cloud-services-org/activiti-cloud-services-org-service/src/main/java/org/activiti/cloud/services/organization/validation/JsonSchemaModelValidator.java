/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.organization.validation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.error.SyntacticModelValidationException;
import org.apache.commons.collections4.CollectionUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON Schema based abstract implementation for {@link ModelValidator}
 */
public abstract class JsonSchemaModelValidator implements ModelValidator {

    private final Logger log = LoggerFactory.getLogger(JsonSchemaModelValidator.class);

    protected abstract SchemaLoader schemaLoader();

    @Override
    public void validateModelContent(byte[] bytes) {
        try {
            log.debug("Validating json model content: " + new String(bytes));
            schemaLoader()
                    .load()
                    .build()
                    .validate(new JSONObject(new JSONTokener(new String(bytes))));
        } catch (JSONException jsonException) {
            log.error("Syntactic model JSON validation errors encountered",
                      jsonException);
            throw new SyntacticModelValidationException(jsonException);
        } catch (ValidationException validationException) {
            log.error("Semantic model validation errors encountered: " + validationException.toJSON(),
                      validationException);
            throw new SemanticModelValidationException(validationException.getMessage(),
                                                       getValidationErrors(validationException));
        }
    }

    private List<ModelValidationError> getValidationErrors(ValidationException validationException) {
        return getValidationExceptions(validationException)
                .map(this::toModelValidationError)
                .collect(Collectors.toList());
    }

    private Stream<ValidationException> getValidationExceptions(ValidationException validationException) {
        return Optional.ofNullable(validationException.getCausingExceptions())
                .filter(CollectionUtils::isNotEmpty)
                .map(exceptions -> exceptions
                        .stream()
                        .flatMap(this::getValidationExceptions))
                .orElseGet(() -> Stream.of(validationException));
    }

    private ModelValidationError toModelValidationError(ValidationException validationException) {
        ModelValidationError modelValidationError = new ModelValidationError();
        modelValidationError.setWarning(false);
        modelValidationError.setProblem(validationException.getErrorMessage());
        modelValidationError.setDescription(validationException.getMessage());
        Optional.ofNullable(validationException.getViolatedSchema())
                .map(Schema::getSchemaLocation)
                .ifPresent(modelValidationError::setValidatorSetName);
        return modelValidationError;
    }
}
