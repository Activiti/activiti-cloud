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

package org.activiti.cloud.service.common.batch.api.core.jobexecution;

import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(name = "org.activiti.cloud.service.common.batch.api.core.api.core.controllerAdvice",
        havingValue = "true",
        matchIfMissing = true)
@ControllerAdvice
@Slf4j
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAnyException(Exception e, WebRequest request) {
        log.error("Request {} failed with {}", request, e);
        String message = e.getMessage();
        String causeMessage = "";
        if (e.getCause() != null)
            causeMessage = e.getCause().getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError apiError = new ApiError(status.toString(), message, e.getClass().getSimpleName(), causeMessage);
        return handleExceptionInternal(e, apiError, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(BatchRuntimeException.class)
    protected ResponseEntity<Object> handleBatchRuntimeException(BatchRuntimeException e, WebRequest request) {
        log.error("Request {} failed with {}", request, e);
        Throwable cause = e.getCause();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (cause instanceof DuplicateJobException ||
                cause instanceof JobExecutionAlreadyRunningException ||
                cause instanceof JobInstanceAlreadyCompleteException)
            status = HttpStatus.CONFLICT;
        else if (cause instanceof JobParametersInvalidException ||
                cause instanceof JobParametersNotFoundException)
            status = HttpStatus.BAD_REQUEST;
        else if (cause instanceof NoSuchJobException ||
                cause instanceof NoSuchJobExecutionException ||
                cause instanceof NoSuchJobInstanceException)
            status = HttpStatus.NOT_FOUND;

        ApiError apiError = new ApiError(status.toString(),
                                         cause.getMessage(),
                                         cause.getClass().getSimpleName(),
                                         e.getMessage());
        return handleExceptionInternal(e, apiError, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(javax.batch.operations.NoSuchJobExecutionException.class)
    protected ResponseEntity<Object> handleNoSuchJobExecutionException(
                                                                       javax.batch.operations.NoSuchJobExecutionException e,
                                                                       WebRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return handleExceptionInternal(e,
                                       new ApiError(status.toString(),
                                                    e.getMessage(),
                                                    e.getClass().getSimpleName(),
                                                    ""),
                                       new HttpHeaders(),
                                       status,
                                       request);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public class ApiError {
        String status;
        String message;
        String exception;
        String detail;
    }
}