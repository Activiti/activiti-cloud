/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.rest;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.activiti.cloud.services.query.app.repository.QueryEntityNotFoundException;
import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.cloud.common.error.attributes.ErrorAttributesMessageSanitizer;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonExceptionHandlerQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExceptionHandlerQuery.class);

    // TODO: 12/04/2019 this exception handler should be moved to activiti-cloud-service-common
    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        ActivitiForbiddenException ex,
        HttpServletResponse response
    ) {
        response.setContentType("application/json");
        return EntityModel.of(new ActivitiErrorMessageImpl(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler({ IllegalStateException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        IllegalStateException ex,
        HttpServletResponse response
    ) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler({ ConversionFailedException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        ConversionFailedException ex,
        HttpServletResponse response
    ) {
        LOGGER.warn(ex.getMessage(), ex);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return EntityModel.of(
            new ActivitiErrorMessageImpl(
                HttpStatus.BAD_REQUEST.value(),
                ErrorAttributesMessageSanitizer.ERROR_NOT_DISCLOSED_MESSAGE
            )
        );
    }

    @ExceptionHandler(QueryEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        QueryEntityNotFoundException ex,
        HttpServletResponse response
    ) {
        LOGGER.debug(ex.getMessage(), ex);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        EntityNotFoundException ex,
        HttpServletResponse response
    ) {
        LOGGER.debug(ex.getMessage(), ex);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }
}
