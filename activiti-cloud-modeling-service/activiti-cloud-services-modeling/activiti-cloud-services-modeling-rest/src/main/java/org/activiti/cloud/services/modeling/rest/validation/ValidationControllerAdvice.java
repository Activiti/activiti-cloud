/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.modeling.rest.validation;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Controller advice for handling validators
 */
@ControllerAdvice
public class ValidationControllerAdvice {

    @InitBinder("model")
    public void initModelBinder(final WebDataBinder binder,
                                final HttpServletRequest request) {
        binder.addValidators(new ModelPayloadValidator(isPost(request)));
    }

    @InitBinder("project")
    public void initProjectBinder(final WebDataBinder binder,
                                final HttpServletRequest request) {
        binder.addValidators(new ProjectPayloadValidator(isPost(request)));
    }

    private boolean isPost(final HttpServletRequest request) {
        return HttpMethod.POST.name().equals(request.getMethod());
    }
}
