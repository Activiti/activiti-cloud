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
package org.activiti.cloud.services.modeling.validation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidator;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.core.error.SyntacticModelValidationException;
import org.apache.commons.collections4.CollectionUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.node.TextNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON Schema based abstract implementation for {@link ModelValidator}
 */
public abstract class JsonSchemaModelValidator implements ModelValidator {

    private ObjectMapper mapper;
    private final Logger log = LoggerFactory.getLogger(JsonSchemaModelValidator.class);

    protected abstract SchemaLoader schemaLoader();

    @Override
    public void validate(byte[] bytes,
                         ValidationContext validationContext) {

        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        ObjectNode processExtensionJson = null;
        try {
            log.debug("Validating json model content: " + new String(bytes));
            processExtensionJson = mapper.readValue(bytes, ObjectNode.class);
            schemaLoader()
                    .load()
                    .build()
                    .validate(processExtensionJson);
        } catch (JsonParseException jsonException) {
            log.error("Syntactic model JSON validation errors encountered",
                      jsonException);
            throw new SyntacticModelValidationException(jsonException);
        } catch (IOException ioException) {
            log.error("Semantic model validation errors encountered: " ,ioException);
            throw new SyntacticModelValidationException(ioException);

        }
    }

//    private List<ModelValidationError> getValidationErrors(JsonParseException validationException, ObjectNode prcessExtenstionJson) {
//        return getValidationExceptions(validationException)
//                .map(exception -> this.toModelValidationError(exception, prcessExtenstionJson))
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    private Stream<ValidationException> getValidationExceptions(ValidationException validationException) {
//        return Optional.ofNullable(validationException.getCausingExceptions())
//                .filter(CollectionUtils::isNotEmpty)
//                .map(exceptions -> exceptions
//                        .stream()
//                        .flatMap(this::getValidationExceptions))
//                .orElseGet(() -> Stream.of(validationException));
//    }

    private ModelValidationError toModelValidationError(ValidationException validationException, ObjectNode prcessExtenstionJson) {
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

    private String resolveExpression(String message, String  pointerToViolation, ObjectNode prcessExtenstionJson) {
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

    private String getValueFromJson(String[] path, ObjectNode processExtensionJson) {
        ObjectNode parent = null;
        String value = "";

        if(path.length > 1) {
            parent = (ObjectNode)processExtensionJson.get(path[0]);

            for(int iterator=1; iterator < path.length - 1; iterator++) {
                parent = (ObjectNode)parent.get(path[iterator]);
            }

            value = ((TextNode)parent.get(path[path.length - 1])).asText();
        }

        if (path.length == 1) {
            value = ((TextNode)processExtensionJson.get(path[0])).asText();
        }

        return  value;
    }
}
