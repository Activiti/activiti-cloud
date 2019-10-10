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

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;

import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.error.SyntacticModelValidationException;
import org.activiti.cloud.services.organization.converter.ProcessModelContentConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

/**
 * {@link ModelValidator} implementation of process models
 */
public class ProcessModelValidator implements ModelValidator {

    private final Logger log = LoggerFactory.getLogger(ProcessModelValidator.class);

    private final ProcessModelType processModelType;

    private final Set<BpmnModelValidator> mpmnModelValidators;

    private final ProcessModelContentConverter processModelContentConverter;

    public ProcessModelValidator(ProcessModelType processModelType,
                                 Set<BpmnModelValidator> mpmnModelValidators,
                                 ProcessModelContentConverter processModelContentConverter) {
        this.processModelType = processModelType;
        this.mpmnModelValidators = mpmnModelValidators;
        this.processModelContentConverter = processModelContentConverter;
    }

    @Override
    public void validateModelContent(byte[] bytes,
                                     ValidationContext validationContext) {
        BpmnModel bpmnModel = processContentToBpmnModel(bytes);
        List<ModelValidationError> validationErrors =
                mpmnModelValidators
                        .stream()
                        .flatMap(mpmnModelValidator -> mpmnModelValidator.validate(bpmnModel,
                                                                                   validationContext))
                        .collect(Collectors.toList());

        if (!validationErrors.isEmpty()) {
            String messageError = "Semantic process model validation errors encountered: " + validationErrors;
            log.error(messageError);
            throw new SemanticModelValidationException(messageError,
                                                       validationErrors);
        }
    }

    private BpmnModel processContentToBpmnModel(byte[] processContent) {
        try {
            return processModelContentConverter.convertToBpmnModel(processContent);
        } catch (IOException | XMLStreamException | XMLException ex) {
            Throwable errorCause = Optional.ofNullable(ex.getCause())
                    .filter(XMLStreamException.class::isInstance)
                    .orElse(ex);
            String messageError = "Syntactic process model XML validation errors encountered: " + errorCause;
            log.error(messageError);
            throw new SyntacticModelValidationException(messageError,
                                                        errorCause);
        }
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
