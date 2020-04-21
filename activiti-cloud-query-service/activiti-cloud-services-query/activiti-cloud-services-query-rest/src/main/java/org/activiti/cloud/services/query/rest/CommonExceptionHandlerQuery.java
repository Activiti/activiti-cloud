package org.activiti.cloud.services.query.rest;

import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class CommonExceptionHandlerQuery {

    // TODO: 12/04/2019 this exception handler should be moved to activiti-cloud-service-common
    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public EntityModel<ActivitiErrorMessage> handleAppException(ActivitiForbiddenException ex, HttpServletResponse response) {
        response.setContentType("application/json");
        return new EntityModel<>(new ActivitiErrorMessageImpl(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }
}

