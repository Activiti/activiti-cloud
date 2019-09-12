package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class RuntimeBundleExceptionHandler {

    @ExceptionHandler(ActivitiObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Resource<ActivitiErrorMessage>  handleAppException(ActivitiObjectNotFoundException ex, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return new Resource<>(new ActivitiErrorMessageImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(ActivitiInterchangeInfoNotFoundException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Resource<ActivitiErrorMessage> handleAppException(ActivitiInterchangeInfoNotFoundException ex, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return new Resource<>(new ActivitiErrorMessageImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Resource<ActivitiErrorMessage> handleAppException(ActivitiForbiddenException ex, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return new Resource<>(new ActivitiErrorMessageImpl(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Resource<ActivitiErrorMessage> handleAppException(NotFoundException ex, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return new Resource<>(new ActivitiErrorMessageImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Resource<ActivitiErrorMessage> handleAppException(IllegalStateException ex, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return new Resource<>(new ActivitiErrorMessageImpl(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }
}
