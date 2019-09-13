/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.validation.process;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ValidationContext;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * Implementation of {@link BpmnModelValidator} for validating process name
 */
@Component
public class BpmnModelNameValidator implements BpmnModelValidator {

    public final String INVALID_NAME = "The process name is invalid";
    public final String INVALID_NAME_DESCRIPTION = "The process name must follow the constraint: '%s'";

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel,
                                                 ValidationContext validationContext) {
        String processName = bpmnModel.getMainProcess().getName();
        List<ModelValidationError> validationErrors = new ArrayList<>();
        if (processName == null) {
            validationErrors.add(createModelValidationError(MODEL_INVALID_NAME_NULL_MESSAGE,
                                                            format(INVALID_NAME_DESCRIPTION,
                                                                   MODEL_INVALID_NAME_EMPTY_MESSAGE),
                                                            INVALID_NAME));
        } else {
            if (isEmpty(processName)) {
                validationErrors.add(createModelValidationError(MODEL_INVALID_NAME_EMPTY_MESSAGE,
                                                                format(INVALID_NAME_DESCRIPTION,
                                                                       MODEL_INVALID_NAME_EMPTY_MESSAGE),
                                                                INVALID_NAME));
            }
            if (processName.length() > NAME_MAX_LENGTH) {
                validationErrors.add(createModelValidationError(MODEL_INVALID_NAME_LENGTH_MESSAGE,
                                                                format(INVALID_NAME_DESCRIPTION,
                                                                       MODEL_INVALID_NAME_LENGTH_MESSAGE),
                                                                INVALID_NAME));
            }
            if (!processName.matches(DNS_LABEL_REGEX)) {
                validationErrors.add(createModelValidationError(MODEL_INVALID_NAME_MESSAGE,
                                                                format(INVALID_NAME_DESCRIPTION,
                                                                       MODEL_INVALID_NAME_MESSAGE),
                                                                INVALID_NAME));
            }
        }
        return validationErrors.stream();
    }
}
