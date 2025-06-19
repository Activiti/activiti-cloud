package org.activiti.cloud.services.query.rest.advice;

import org.activiti.cloud.services.query.rest.TaskAdminController;
import org.activiti.cloud.services.query.rest.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = { TaskController.class, TaskAdminController.class })
public class TaskControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskControllerAdvice.class);

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<String> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException e) {
        LOGGER.warn("Invalid data access in task search: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(String.format("Invalid search parameter: %s", e.getMessage()));
    }
}
