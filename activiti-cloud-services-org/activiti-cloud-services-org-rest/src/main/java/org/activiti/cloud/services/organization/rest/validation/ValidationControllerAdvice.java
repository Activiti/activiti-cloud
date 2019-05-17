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
package org.activiti.cloud.services.organization.rest.validation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.activiti.cloud.organization.api.Model;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Controller advice for handling validators
 */
@ControllerAdvice
public class ValidationControllerAdvice {

    private final Map<String, ModelPayloadValidator> modelMetadataValidatorsMapByModelType;

    public ValidationControllerAdvice(Set<ModelPayloadValidator> modelMetadataValidators) {
        this.modelMetadataValidatorsMapByModelType = modelMetadataValidators
                .stream()
                .collect(Collectors.toMap(validator -> validator.getHandledModelType().getName(),
                                          Function.identity()));
    }

    @InitBinder("model")
    public void initModelBinder(final WebDataBinder binder,
                                final HttpServletRequest request) {
        boolean checkRequiredField = HttpMethod.POST.name().equals(request.getMethod());
        binder.addValidators(new GenericModelPayloadValidator(checkRequiredField));

        Optional.ofNullable(binder.getTarget())
                .filter(target -> Model.class.isAssignableFrom(target.getClass()))
                .map(Model.class::cast)
                .map(Model::getType)
                .map(modelMetadataValidatorsMapByModelType::get)
                .ifPresent(binder::addValidators);
    }
}
