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
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.modeling.converter.BpmnProcessModelContent;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;

public class BpmnModelCallActivityValidator implements BpmnCommonModelValidator {

    private ProcessModelType processModelType;
    private ProcessModelContentConverter processModelContentConverter;
    private final String expressionRegex = "\\$+\\{+.+\\}";
    private final String INVALID_CALL_ACTIVITY_REFERENCE_DESCRIPTION = "Call activity '%s' with call element '%s' found in process '%s' references a process id that does not exist in the current project.";
    private final String INVALID_CALL_ACTIVITY_REFERENCE_PROBLEM = "Call activity element must reference a process id present in the current project.";
    private final String INVALID_CALL_ACTIVITY_REFERENCE_NAME = "Invalid call activity reference validator.";
    private final String NO_REFERENCE_FOR_CALL_ACTIVITY_DESCRIPTION = "No call element found for call activity '%s' found in process '%s'. Call activity must have a call element that reference a process id present in the current project.";
    private final String NO_REFERENCE_FOR_CALL_ACTIVITY_PROBLEM = "No call element found for call activity '%s' in process '%s'";
    private final String NO_REFERENCE_FOR_CALL_ACTIVITY_REFERENCE_NAME = "Call activity must have a call element validator.";

    public BpmnModelCallActivityValidator(ProcessModelType processModelType,
                                          ProcessModelContentConverter processModelContentConverter) {
        this.processModelType = processModelType;
        this.processModelContentConverter = processModelContentConverter;
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel,
                                                 ValidationContext validationContext) {
        Set<String> availableProcessesIds = validationContext.getAvailableModels(processModelType)
                .stream()
                .flatMap(model -> this.retrieveProcessIdFromModel(model))
                .collect(Collectors.toSet());
        return validateCallActivities(availableProcessesIds,
                                      bpmnModel);
    }

    private Stream<String> retrieveProcessIdFromModel(Model model) throws RuntimeException {
        return processModelContentConverter.convertToBpmnModel(model.getContent())
                .getProcesses().stream().map(process -> process.getId());
    }

    private Stream<ModelValidationError> validateCallActivities(Set<String> availableProcessesIds,
                                                                BpmnModel bpmnModel) {
        return processModelContentConverter.convertToModelContent(bpmnModel)
                .map(converter ->
                    this.evaluateProcessCallActivity(converter, availableProcessesIds, bpmnModel))
            .orElse(Stream.empty());
    }

    private Stream<ModelValidationError> evaluateProcessCallActivity(BpmnProcessModelContent converter,
                                                                     Set<String> availableProcessesIds,
                                                                     BpmnModel bpmnModel) {
        Set<CallActivity> availableActivities = converter.findAllNodes(CallActivity.class);
        return availableActivities.stream()
            .map(activity -> validateCallActivity(availableProcessesIds,
                bpmnModel.getMainProcess().getId(),
                activity))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }


    private Optional<ModelValidationError> validateCallActivity(Set<String> availableProcessesIds,
                                                                String mainProcess,
                                                                CallActivity callActivity) {
        String calledElement = callActivity.getCalledElement();

        if (isEmpty(calledElement)) {
            return Optional.of(
                new ModelValidationError(format(NO_REFERENCE_FOR_CALL_ACTIVITY_PROBLEM,
                    callActivity.getId(),
                    mainProcess), format(NO_REFERENCE_FOR_CALL_ACTIVITY_DESCRIPTION,
                    callActivity.getId(),
                    mainProcess), NO_REFERENCE_FOR_CALL_ACTIVITY_REFERENCE_NAME)
            );
        }
        else if (calledElement.matches(expressionRegex)) {
            return Optional.empty();
        }
        else {
            String calledElementId = calledElement.replace("process-",
                                                           "");
            return !availableProcessesIds.contains(calledElementId) ?
                    Optional.of(new ModelValidationError(INVALID_CALL_ACTIVITY_REFERENCE_PROBLEM,
                        format(INVALID_CALL_ACTIVITY_REFERENCE_DESCRIPTION,
                            callActivity.getId(),
                            calledElementId,
                            mainProcess,
                            INVALID_CALL_ACTIVITY_REFERENCE_NAME),
                        INVALID_CALL_ACTIVITY_REFERENCE_NAME)) : Optional.empty();
        }
    }
}
