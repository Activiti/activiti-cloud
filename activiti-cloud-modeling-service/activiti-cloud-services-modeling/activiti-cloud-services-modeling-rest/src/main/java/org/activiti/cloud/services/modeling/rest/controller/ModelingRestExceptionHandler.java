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
package org.activiti.cloud.services.modeling.rest.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;

import org.activiti.cloud.common.error.attributes.ErrorAttributesBuilder;
import org.activiti.cloud.common.error.attributes.ErrorAttributesCustomizer;
import org.activiti.cloud.common.error.attributes.ErrorAttributesMessageSanitizer;
import org.activiti.cloud.modeling.core.error.ImportModelException;
import org.activiti.cloud.modeling.core.error.ImportProjectException;
import org.activiti.cloud.modeling.core.error.ModelConversionException;
import org.activiti.cloud.modeling.core.error.ModelNameConflictException;
import org.activiti.cloud.modeling.core.error.ModelScopeIntegrityException;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.core.error.SyntacticModelValidationException;
import org.activiti.cloud.modeling.core.error.UnknownModelTypeException;
import org.activiti.cloud.services.modeling.rest.controller.error.attributes.ErrorAttributesModelValidationErrorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Handler for REST exceptions
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ModelingRestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModelingRestExceptionHandler.class);

    public static final String DATA_INTEGRITY_VIOLATION_EXCEPTION_MESSAGE = "Data integrity violation";

    public static final String DATA_ACCESS_EXCEPTION_MESSAGE = "Data access error";

    @Bean
    public ErrorAttributes errorAttributes() {

        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                          ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest,
                        options);

                ErrorAttributesCustomizer[] customizers = {
                    new ErrorAttributesModelValidationErrorsBuilder(),
                    new ErrorAttributesMessageSanitizer()
                };

                return ErrorAttributesBuilder.build(errorAttributes, getError(webRequest), customizers);
            }
        };
    }

    @ExceptionHandler({
            UnknownModelTypeException.class,
            SyntacticModelValidationException.class,
            SemanticModelValidationException.class,
            ImportProjectException.class,
            ImportModelException.class,
            ModelConversionException.class
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

    @ExceptionHandler(ModelNameConflictException.class)
    public void handleModelNameConflictException(ModelNameConflictException ex,
        HttpServletResponse response) throws IOException {
        logger.error(ex.getMessage(), ex);
        response.sendError(CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(ModelScopeIntegrityException.class)
    public void handleModelScopeIntegrityException(ModelScopeIntegrityException ex,
        HttpServletResponse response) throws IOException {
        logger.error(ex.getMessage(), ex);
        response.sendError(CONFLICT.value(), ex.getMessage());
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
}
