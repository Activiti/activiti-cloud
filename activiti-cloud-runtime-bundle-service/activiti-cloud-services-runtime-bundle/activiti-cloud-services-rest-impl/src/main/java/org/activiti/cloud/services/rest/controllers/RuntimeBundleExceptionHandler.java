package org.activiti.cloud.services.rest.controllers;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.servlet.http.HttpServletResponse;
import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.UnprocessableEntityException;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RuntimeBundleExceptionHandler {

    @ExceptionHandler(ActivitiInterchangeInfoNotFoundException.class)
    @ResponseStatus(NO_CONTENT)
    public EntityModel<ActivitiErrorMessage> handleAppException(ActivitiInterchangeInfoNotFoundException ex, HttpServletResponse response) {
        response.setContentType(APPLICATION_JSON_VALUE);
        return new EntityModel<>(new ActivitiErrorMessageImpl(NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(FORBIDDEN)
    public EntityModel<ActivitiErrorMessage> handleAppException(ActivitiForbiddenException ex, HttpServletResponse response) {
        response.setContentType(APPLICATION_JSON_VALUE);
        return new EntityModel<>(new ActivitiErrorMessageImpl(FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(UNPROCESSABLE_ENTITY)
    public EntityModel<ActivitiErrorMessage> handleAppException(UnprocessableEntityException ex, HttpServletResponse response) {
        response.setContentType(APPLICATION_JSON_VALUE);
        return new EntityModel<>(new ActivitiErrorMessageImpl(UNPROCESSABLE_ENTITY.value(), ex.getMessage()));
    }

    @ExceptionHandler({NotFoundException.class, ActivitiObjectNotFoundException.class})
    @ResponseStatus(NOT_FOUND)
    public EntityModel<ActivitiErrorMessage> handleAppException(RuntimeException ex, HttpServletResponse response) {
        response.setContentType(APPLICATION_JSON_VALUE);
        return new EntityModel<>(new ActivitiErrorMessageImpl(NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(BAD_REQUEST)
    public EntityModel<ActivitiErrorMessage> handleAppException(IllegalStateException ex, HttpServletResponse response) {
        response.setContentType(APPLICATION_JSON_VALUE);
        return new EntityModel<>(new ActivitiErrorMessageImpl(BAD_REQUEST.value(), ex.getMessage()));
    }
}
