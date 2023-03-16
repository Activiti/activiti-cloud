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
package org.activiti.cloud.services.modeling.validation.process;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.api.error.ModelingException;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidator;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ModelValidator} implementation of process models
 */
public class ProcessModelValidator implements ModelContentValidator {

    private final Logger log = LoggerFactory.getLogger(ProcessModelValidator.class);

    private final ProcessModelType processModelType;

    private final Set<BpmnCommonModelValidator> bpmnCommonModelValidators;

    private final ProcessModelContentConverter processModelContentConverter;

    public ProcessModelValidator(
        ProcessModelType processModelType,
        Set<BpmnCommonModelValidator> bpmnCommonModelValidators,
        ProcessModelContentConverter processModelContentConverter
    ) {
        this.processModelType = processModelType;
        this.bpmnCommonModelValidators = bpmnCommonModelValidators;
        this.processModelContentConverter = processModelContentConverter;
    }

    @Override
    public void validate(byte[] bytes, ValidationContext validationContext) {
        BpmnModel bpmnModel = processModelContentConverter.convertToBpmnModel(bytes);

        List<ModelValidationError> validationErrors = bpmnCommonModelValidators
            .stream()
            .flatMap(bpmnCommonModelValidator -> bpmnCommonModelValidator.validate(bpmnModel, validationContext))
            .collect(Collectors.toList());

        if (!validationErrors.isEmpty()) {
            String messageError = "Semantic process model validation errors encountered: " + validationErrors;
            log.debug(messageError);
            throw new SemanticModelValidationException(messageError, validationErrors);
        }
    }

    @Override
    public Collection<ModelValidationError> validateAndReturnErrors(
        byte[] modelContent,
        ValidationContext validationContext
    ) {
        return null;
    }

    @Override
    public ModelType getHandledModelType() {
        return processModelType;
    }

    @Override
    public String getHandledContentType() {
        return CONTENT_TYPE_XML;
    }
}
