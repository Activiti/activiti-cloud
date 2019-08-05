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
package org.activiti.cloud.services.organization.validation;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;

import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.Task;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.error.SyntacticModelValidationException;
import org.activiti.cloud.services.organization.converter.ConnectorModelAction;
import org.activiti.cloud.services.organization.converter.ConnectorModelContent;
import org.activiti.cloud.services.organization.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.organization.converter.ProcessModelContentConverter;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ValidationError;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * {@link ModelValidator} implementation of process models
 */
@Component
@ConditionalOnMissingBean(name = "ProcessModelValidator")
public class ProcessModelValidator implements ModelValidator {

    private final Logger log = LoggerFactory.getLogger(ProcessModelValidator.class);

    private final ProcessModelType processModelType;

    private final ProcessValidator processValidator;

    private final ProcessModelContentConverter processModelContentConverter;
    private final ConnectorModelContentConverter connectorModelContentConverter;

    public final String NO_ASSIGNEE_PROBLEM_TITLE = "No assignee for user task";
    public final String NO_ASSIGNEE_DESCRIPTION = "One of the attributes 'assignee','candidateUsers' or 'candidateGroups' are mandatory on user task";
    public final String USER_TASK_VALIDATOR_NAME = "BPMN user task validator";

    public final String INVALID_SERVICE_IMPLEMENTATION_PROBLEM = "Invalid service implementation";
    public final String INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION = "Invalid service implementation on service '%s'";
    public final String SERVICE_USER_TASK_VALIDATOR_NAME = "BPMN service task validator";

    @Autowired
    public ProcessModelValidator(ProcessModelType processModelType,
                                 ProcessValidator processValidator,
                                 ProcessModelContentConverter processModelContentConverter,
                                 ConnectorModelContentConverter connectorModelContentConverter) {
        this.processModelType = processModelType;
        this.processValidator = processValidator;
        this.processModelContentConverter = processModelContentConverter;
        this.connectorModelContentConverter = connectorModelContentConverter;
    }

    @Override
    public void validateModelContent(byte[] bytes,
                                     ValidationContext validationContext) {
        try {
            BpmnModel bpmnModel = processModelContentConverter.convertToBpmnModel(bytes);
            List<ValidationError> validationErrors = processValidator.validate(bpmnModel);
            Stream<Optional<ValidationError>> validationErrorsStream = validateModelContentInCurrentContext(bpmnModel,
                                                                                                            validationContext);

            validationErrors.addAll(validationErrorsStream.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
            if (!validationErrors.isEmpty()) {
                log.error("Semantic process model validation errors encountered: " + validationErrors);
                throw new SemanticModelValidationException(validationErrors
                                                                   .stream()
                                                                   .map(this::toModelValidationError)
                                                                   .collect(Collectors.toList()));
            }
        } catch (IOException | XMLStreamException | XMLException ex) {
            Throwable errorCause = Optional.ofNullable(ex.getCause())
                    .filter(XMLStreamException.class::isInstance)
                    .orElse(ex);
            log.error("Syntactic process model XML validation errors encountered: " + errorCause);
            throw new SyntacticModelValidationException(errorCause);
        }
    }

    protected Stream<Optional<ValidationError>> validateModelContentInCurrentContext(BpmnModel bpmnModel,
                                                                                     ValidationContext validationContext) {
        Stream<Optional<ValidationError>> validationErrorsStream = validateUserTasksAssignee(bpmnModel);

        if (!(validationContext instanceof ModelValidationContext)) {
            validationErrorsStream = Stream.concat(validationErrorsStream,
                                                   validateServiceTasksImplementation(bpmnModel,
                                                                                      validationContext));
        }
        return validationErrorsStream;
    }

    private Stream<Optional<ValidationError>> validateServiceTasksImplementation(BpmnModel bpmnModel,
                                                                                 ValidationContext validationContext) {
        List<String> availableImplementations = getAvailableImplementations(validationContext);
        //TODO: hardcoded decision table added -> fix this after implementation for decision table will change
        availableImplementations.add("dmn-connector.EXECUTE_TABLE");

        return getTasksStream(bpmnModel,
                              ServiceTask.class)
                .map(serviceTask -> validateServiceTaskImplementation(serviceTask,
                                                                      availableImplementations))
                .filter(Optional::isPresent);
    }

    protected <T extends Task> Stream<T> getTasksStream(BpmnModel bpmnModel,
                                                        Class<T> taskType) {
        return bpmnModel.getProcesses()
                .stream()
                .flatMap(process -> process.getFlowElements().stream())
                .filter(element -> taskType.isAssignableFrom(element.getClass()))
                .map(taskType::cast);
    }

    private List<String> getAvailableImplementations(ValidationContext validationContext) {
        return validationContext.getAvailableModels()
                .stream()
                .filter(model -> ConnectorModelType.NAME.equals(model.getType()))
                .map(this::concatNameAndActions)
                .flatMap(Stream::sorted)
                .collect(Collectors.toList());
    }

    private Stream<String> concatNameAndActions(Model model) {
        return extractConnectorModelContent(model).map(connectorModelContent -> connectorModelContent
                .getActions()
                .values()
                .stream()
                .map(connectorModelAction -> concatNameAndAction(connectorModelAction,
                                                                 model)))
                .orElse(Stream.empty());
    }

    private Optional<ConnectorModelContent> extractConnectorModelContent(Model model) {
        return Optional.ofNullable(model.getContent())
                .map(String::getBytes)
                .flatMap(connectorModelContentConverter::convertToModelContent)
                .filter(connectorModelContent -> connectorModelContent.getActions() != null);
    }

    private String concatNameAndAction(ConnectorModelAction connectorModelAction,
                                       Model model) {
        return isEmpty(connectorModelAction) && isEmpty(connectorModelAction.getName()) ? model.getName() : model.getName() + "." + connectorModelAction.getName();
    }

    private Optional<ValidationError> validateServiceTaskImplementation(ServiceTask serviceTask,
                                                                        List<String> availableImplementations) {

        ValidationError validationError = new ValidationError();
        validationError.setProblem(INVALID_SERVICE_IMPLEMENTATION_PROBLEM);
        validationError.setValidatorSetName(SERVICE_USER_TASK_VALIDATOR_NAME);
        validationError.setDefaultDescription(String.format(INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION,
                                                            serviceTask.getId()));

        return availableImplementations
                .stream()
                .anyMatch(implementation -> implementation.equals(serviceTask.getImplementation())) ?
                Optional.empty() : Optional.of(validationError);
    }

    private Stream<Optional<ValidationError>> validateUserTasksAssignee(BpmnModel bpmnModel) {
        return getTasksStream(bpmnModel,
                              UserTask.class)
                .map(this::validateTaskAssignedUser)
                .filter(Optional::isPresent);
    }

    private Optional<ValidationError> validateTaskAssignedUser(UserTask userTask) {

        if (!(isEmpty(userTask.getAssignee()) && CollectionUtils.isEmpty(userTask.getCandidateUsers()) && CollectionUtils.isEmpty(userTask.getCandidateGroups()))) {
            return Optional.empty();
        }

        ValidationError validationError = new ValidationError();

        validationError.setProblem(NO_ASSIGNEE_PROBLEM_TITLE);
        validationError.setValidatorSetName(USER_TASK_VALIDATOR_NAME);
        validationError.setDefaultDescription(NO_ASSIGNEE_DESCRIPTION);

        return Optional.of(validationError);
    }

    private ModelValidationError toModelValidationError(ValidationError validationError) {
        ModelValidationError modelValidationError = new ModelValidationError();
        modelValidationError.setWarning(validationError.isWarning());
        modelValidationError.setProblem(validationError.getProblem());
        modelValidationError.setDescription(validationError.getDefaultDescription());
        modelValidationError.setValidatorSetName(validationError.getValidatorSetName());
        return modelValidationError;
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
