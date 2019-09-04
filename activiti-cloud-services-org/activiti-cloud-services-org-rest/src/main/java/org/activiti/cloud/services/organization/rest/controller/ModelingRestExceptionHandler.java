/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.rest.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;

import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.core.error.ImportModelException;
import org.activiti.cloud.organization.core.error.ImportProjectException;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.error.SyntacticModelValidationException;
import org.activiti.cloud.organization.core.error.UnknownModelTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Handler for REST exceptions
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ModelingRestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModelingRestExceptionHandler.class);

    public static final String ERRORS = "errors";

    public static final String DATA_INTEGRITY_VIOLATION_EXCEPTION_MESSAGE = "Data integrity violation";

    public static final String DATA_ACCESS_EXCEPTION_MESSAGE = "Data access error";

    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                          boolean includeStackTrace) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest,
                                                                               includeStackTrace);
                Stream<ModelValidationError> bindingErrors = Optional.ofNullable((List<ObjectError>) errorAttributes.get("errors"))
                        .map(this::transformBindingErrors)
                        .orElse(Stream.empty());
                Stream<ModelValidationError> semanticErrors = resolveSemanticErrors(webRequest,
                                                                                    errorAttributes);
                Stream<ModelValidationError> modelValidationErrorStream = Stream.concat(bindingErrors,
                                                                                        semanticErrors);
                List<ModelValidationError> collectedErrors = modelValidationErrorStream.collect(Collectors.toList());
                if (!collectedErrors.isEmpty()) {
                    errorAttributes.put(ERRORS,
                                        collectedErrors);
                }
                return errorAttributes;
            }

            private Stream<ModelValidationError> resolveSemanticErrors(WebRequest webRequest,
                                                                       Map<String, Object> errorAttributes) {
                return Optional.ofNullable(getError(webRequest))
                        .filter(SemanticModelValidationException.class::isInstance)
                        .map(SemanticModelValidationException.class::cast)
                        .map(SemanticModelValidationException::getValidationErrors)
                        .map(Collection::stream)
                        .orElse(Stream.empty());
            }

            private Stream<ModelValidationError> transformBindingErrors(List<ObjectError> errors) {
                return errors.stream()
                        .map(error -> createModelValidationError(error));
            }
        };
    }

    @ExceptionHandler({
            UnknownModelTypeException.class,
            SyntacticModelValidationException.class,
            SemanticModelValidationException.class,
            ImportProjectException.class,
            ImportModelException.class
    })
    public void handleBadRequestException(Exception ex,
                                          HttpServletResponse response) throws IOException {
        logger.error(ex.getMessage(),
                     ex);
        response.sendError(BAD_REQUEST.value(),
                           ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public void handleDataIntegrityViolationException(DataIntegrityViolationException ex,
                                                      HttpServletResponse response) throws IOException {
        logger.error(DATA_INTEGRITY_VIOLATION_EXCEPTION_MESSAGE,
                     ex);
        response.sendError(CONFLICT.value(),
                           DATA_INTEGRITY_VIOLATION_EXCEPTION_MESSAGE);
    }

    @ExceptionHandler({
            DataAccessException.class,
            PersistenceException.class,
            SQLException.class
    })
    public void handleDataAccessException(Exception ex,
                                          HttpServletResponse response) throws IOException {
        logger.error(DATA_ACCESS_EXCEPTION_MESSAGE,
                     ex);
        response.sendError(INTERNAL_SERVER_ERROR.value(),
                           DATA_ACCESS_EXCEPTION_MESSAGE);
    }

    private ModelValidationError createModelValidationError(ObjectError objectError) {
        ModelValidationError modelValidationError = new ModelValidationError();
        modelValidationError.setWarning(false);
        modelValidationError.setProblem(objectError.getCode());
        modelValidationError.setDescription(objectError.getDefaultMessage());
        modelValidationError.setValidatorSetName(objectError.getObjectName());
        return modelValidationError;
    }
}
