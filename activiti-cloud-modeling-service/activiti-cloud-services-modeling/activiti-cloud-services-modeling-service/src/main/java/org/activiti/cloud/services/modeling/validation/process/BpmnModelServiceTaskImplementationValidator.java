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

import static java.lang.String.format;
import static org.springframework.util.StringUtils.isEmpty;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContent;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.modeling.converter.ConnectorModelFeature;

/**
 * Implementation of {@link BpmnModelValidator} vor validating service task implementation
 */
public class BpmnModelServiceTaskImplementationValidator implements BpmnModelValidator {

    public static final String INVALID_SERVICE_IMPLEMENTATION_PROBLEM = "Invalid service implementation";
    public static final String INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION = "Invalid service implementation on service '%s'";
    public static final String SERVICE_USER_TASK_VALIDATOR_NAME = "BPMN service task validator";
    private static final Pattern VALID_SERVICE_TASK_EVENT_REGEX = Pattern.compile("^([a-z]+)-service\\.([a-zA-Z-_]+)");

    private final ConnectorModelType connectorModelType;

    private final ConnectorModelContentConverter connectorModelContentConverter;

    public BpmnModelServiceTaskImplementationValidator(ConnectorModelType connectorModelType,
            ConnectorModelContentConverter connectorModelContentConverter) {
        this.connectorModelType = connectorModelType;
        this.connectorModelContentConverter = connectorModelContentConverter;
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel,
                                                 ValidationContext validationContext) {
        List<String> availableImplementations = getAvailableImplementations(validationContext);
        //TODO: hardcoded decision table added -> fix this after implementation for decision table will change
        availableImplementations.add("dmn-connector.EXECUTE_TABLE");
        availableImplementations.add("script.EXECUTE");
        
        return getFlowElements(bpmnModel,
                        ServiceTask.class)
                .filter(serviceTask -> serviceTask.getImplementation() != null)
                .map(serviceTask -> validateServiceTaskImplementation(serviceTask,
                                                                      availableImplementations))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private List<String> getAvailableImplementations(ValidationContext validationContext) {
        return validationContext.getAvailableModels(connectorModelType)
                .stream()
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
                .flatMap(connectorModelContentConverter::convertToModelContent)
                .filter(connectorModelContent -> connectorModelContent.getActions() != null);
    }

    private String concatNameAndAction(ConnectorModelFeature connectorModelFeature,
                                       Model model) {
        return isEmpty(connectorModelFeature) && isEmpty(connectorModelFeature.getName()) ?
                model.getName() :
                model.getName() + "." + connectorModelFeature.getName();
    }

    private Optional<ModelValidationError> validateServiceTaskImplementation(ServiceTask serviceTask,
            List<String> availableImplementations) {
        if (isValidImplementation(serviceTask.getImplementation(), availableImplementations)) {
            return Optional.<ModelValidationError>empty();
        }
        return Optional.of(
                new ModelValidationError(INVALID_SERVICE_IMPLEMENTATION_PROBLEM,
                        format(INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION,
                                serviceTask.getId()), SERVICE_USER_TASK_VALIDATOR_NAME));
    }

    private boolean isValidImplementation(String implementation, List<String> availableImplementations) {
        return VALID_SERVICE_TASK_EVENT_REGEX.matcher(implementation).matches() || availableImplementations.contains(implementation);
    }
}
