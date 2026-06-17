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
package org.activiti.cloud.services.rest.controllers;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.UnprocessableEntityException;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RuntimeBundleExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeBundleExceptionHandler.class);

    @ExceptionHandler(ActivitiInterchangeInfoNotFoundException.class)
    @ResponseStatus(NO_CONTENT)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        ActivitiInterchangeInfoNotFoundException ex,
        HttpServletResponse response
    ) {
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(FORBIDDEN)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        ActivitiForbiddenException ex,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logFailedRequest(request, ex);
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(UNPROCESSABLE_ENTITY)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        UnprocessableEntityException ex,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logFailedRequest(request, ex);
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(UNPROCESSABLE_ENTITY.value(), ex.getMessage()));
    }

    @ExceptionHandler({ NotFoundException.class, ActivitiObjectNotFoundException.class })
    @ResponseStatus(NOT_FOUND)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        RuntimeException ex,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logFailedRequest(request, ex);
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(BAD_REQUEST)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        IllegalStateException ex,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logFailedRequest(request, ex);
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(ActivitiException.class)
    @ResponseStatus(BAD_REQUEST)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        ActivitiException ex,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logFailedRequest(request, ex);
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(ActivitiIllegalArgumentException.class)
    @ResponseStatus(CONFLICT)
    public EntityModel<ActivitiErrorMessage> handleAppException(
        ActivitiIllegalArgumentException ex,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logFailedRequest(request, ex);
        response.setContentType(APPLICATION_JSON_VALUE);
        return EntityModel.of(new ActivitiErrorMessageImpl(CONFLICT.value(), ex.getMessage()));
    }

    private void logFailedRequest(HttpServletRequest request, Exception ex) {
        LOGGER.warn("{} {} failed", request.getMethod(), request.getRequestURI(), ex);
    }
}
