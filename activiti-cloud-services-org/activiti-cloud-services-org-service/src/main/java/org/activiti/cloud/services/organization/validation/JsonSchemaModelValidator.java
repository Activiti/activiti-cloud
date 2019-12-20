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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.api.ValidationContext;
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
    public void validate(byte[] bytes,
                         ValidationContext validationContext) {
        JSONObject processExtensionJson = null;
        try {
            log.debug("Validating json model content: " + new String(bytes));
            processExtensionJson = new JSONObject(new JSONTokener(new String(bytes)));
            schemaLoader()
                    .load()
                    .build()
                    .validate(processExtensionJson);
        } catch (JSONException jsonException) {
            log.error("Syntactic model JSON validation errors encountered",
                      jsonException);
            throw new SyntacticModelValidationException(jsonException);
        } catch (ValidationException validationException) {
            log.error("Semantic model validation errors encountered: " + validationException.toJSON(),
                      validationException);
            throw new SemanticModelValidationException(validationException.getMessage(),
                                                       getValidationErrors(validationException, processExtensionJson));
        }
    }

    private List<ModelValidationError> getValidationErrors(ValidationException validationException, JSONObject prcessExtenstionJson) {
        return getValidationExceptions(validationException)
                .map(exception -> this.toModelValidationError(exception, prcessExtenstionJson))
                .distinct()
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

    private ModelValidationError toModelValidationError(ValidationException validationException, JSONObject prcessExtenstionJson) {
        String description = null;

        Map<String, Object>  unProcessedProperties = Optional.ofNullable(validationException.getViolatedSchema())
                .map(Schema::getUnprocessedProperties).orElse(null);

        if(unProcessedProperties != null && unProcessedProperties.get("message") != null){
            HashMap<String, String> errorMessages = (HashMap<String, String>) unProcessedProperties.get("message");
            String errorMessage = errorMessages.get(validationException.getKeyword());
            if(errorMessage != null) {
                description = resolveExpression(errorMessages.get(validationException.getKeyword()), validationException.getPointerToViolation(), prcessExtenstionJson);
            }
        }

        if(description == null || description.equals("")) {
            // set default value if custom error message not found
            description = validationException.getMessage();
        }

        String schema = Optional.ofNullable(validationException.getViolatedSchema())
                .map(Schema::getSchemaLocation)
                .orElse(null);
        return createModelValidationError(validationException.getErrorMessage(),
                                          description,
                                          schema);
    }

    private String resolveExpression(String message, String  pointerToViolation, JSONObject prcessExtenstionJson) {
        final String path[] = pointerToViolation.replace("#/", "").split("/");
        final int lastIndex = path.length - 1;

        if (message.contains("{{name}}")) {
            path[lastIndex] = "name";
            message = message.replace("{{name}}", getValueFromJson(path, prcessExtenstionJson));
        } else if (message.contains("{{id}}")) {
            path[lastIndex] = "id";
            message = message.replace("{{id}}", getValueFromJson(path, prcessExtenstionJson));
        }

        if (message.matches(".*\\{\\{(name|id)\\}\\}.*")) {
            return resolveExpression(message, pointerToViolation, prcessExtenstionJson);
        }

        return  message;
    }

    private String getValueFromJson(String[] path, JSONObject processExtensionJson) {
        JSONObject parent = null;
        String value = "";

        if(path.length > 1) {
            parent = processExtensionJson.getJSONObject(path[0]);

            for(int iterator=1; iterator < path.length - 1; iterator++) {
                parent = parent.getJSONObject(path[iterator]);
            }

            value = parent.getString(path[path.length - 1]);
        }

        if (path.length == 1) {
            value = processExtensionJson.getString(path[0]);
        }

        return  value;
    }
}
