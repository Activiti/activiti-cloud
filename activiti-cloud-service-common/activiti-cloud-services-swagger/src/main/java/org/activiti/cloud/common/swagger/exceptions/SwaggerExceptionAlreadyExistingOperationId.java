package org.activiti.cloud.common.swagger.exceptions;

public class SwaggerExceptionAlreadyExistingOperationId extends RuntimeException {

    public static final String EXCEPTION_MESSAGE_PATTERN = "Operation id must be unique, '%s' is already present in this swagger definition";

    public SwaggerExceptionAlreadyExistingOperationId(String message) {
        super(message);
    }
}
