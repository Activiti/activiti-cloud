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
package org.activiti.cloud.identity.web.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.activiti.cloud.identity.exceptions.IdentityInvalidApplicationException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidGroupException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidGroupRoleException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidRoleException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidUserException;
import org.activiti.cloud.identity.exceptions.IdentityInvalidUserRoleException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityManagementRestExceptionHandler {

    @ExceptionHandler(
        {
            IdentityInvalidUserRoleException.class,
            IdentityInvalidUserException.class,
            IdentityInvalidRoleException.class,
            IdentityInvalidGroupException.class,
            IdentityInvalidGroupRoleException.class
        }
    )
    @ResponseStatus(BAD_REQUEST)
    public void handleAppException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(IdentityInvalidApplicationException.class)
    @ResponseStatus(NOT_FOUND)
    public void handleAppException(IdentityInvalidApplicationException ex, HttpServletResponse response)
        throws IOException {
        response.sendError(NOT_FOUND.value(), ex.getMessage());
    }
}
