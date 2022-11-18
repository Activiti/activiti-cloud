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
package org.activiti.cloud.services.query.rest;

import javax.servlet.http.HttpServletResponse;
import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionHandlerQuery {

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
}
