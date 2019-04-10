package org.activiti.cloud.services.error;

import org.activiti.api.model.shared.model.ActivitiError;
import org.activiti.api.runtime.model.impl.ActivitiErrorImpl;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Resource<ActivitiError> handleAppException(ActivitiForbiddenException ex) {
        return new Resource<>(new ActivitiErrorImpl(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Resource<ActivitiError> handleAppException(IllegalStateException ex) {
        return new Resource<>(new ActivitiErrorImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Resource<ActivitiError> handleAppException(NotFoundException ex) {
        return new Resource<>(new ActivitiErrorImpl(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

}
