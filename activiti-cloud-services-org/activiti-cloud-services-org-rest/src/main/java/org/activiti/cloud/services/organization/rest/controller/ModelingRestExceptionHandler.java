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
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import org.activiti.cloud.organization.core.error.ImportApplicationException;
import org.activiti.cloud.organization.core.error.ImportModelException;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.error.SyntacticModelValidationException;
import org.activiti.cloud.organization.core.error.UnknownModelTypeException;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Handler for REST exceptions
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ModelingRestExceptionHandler {

    public static final String ERRORS = "errors";

    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                          boolean includeStackTrace) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest,
                                                                               includeStackTrace);
                Optional.ofNullable(getError(webRequest))
                        .filter(SemanticModelValidationException.class::isInstance)
                        .map(SemanticModelValidationException.class::cast)
                        .map(SemanticModelValidationException::getValidationErrors)
                        .ifPresent(errors -> errorAttributes.put(ERRORS,
                                                                 errors));
                return errorAttributes;
            }
        };
    }

    @ExceptionHandler({
            UnknownModelTypeException.class,
            SyntacticModelValidationException.class,
            SemanticModelValidationException.class,
            ImportApplicationException.class,
            ImportModelException.class
    })
    public void handleBadRequestException(Exception ex,
                                          HttpServletResponse response) throws IOException {
        response.sendError(BAD_REQUEST.value(),
                           ex.getMessage());
    }
}
