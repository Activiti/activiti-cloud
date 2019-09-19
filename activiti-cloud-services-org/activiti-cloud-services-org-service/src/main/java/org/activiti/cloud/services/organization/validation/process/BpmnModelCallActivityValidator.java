package org.activiti.cloud.services.organization.validation.process;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.services.organization.converter.ProcessModelContentConverter;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class BpmnModelCallActivityValidator implements BpmnModelValidator {

    private ProcessModelType processModelType;
    private ProcessModelContentConverter processModelContentConverter;
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
                .map(Model::getId)
                .collect(Collectors.toSet());
        return validateCallActivities(availableProcessesIds,
                                      bpmnModel);
    }

    private Stream<ModelValidationError> validateCallActivities(Set<String> availableProcessesIds,
                                                                BpmnModel bpmnModel) {
        return processModelContentConverter.convertToModelContent(bpmnModel)
                .map(
                        converter -> {
                            Set<CallActivity> availableActivities = converter.findAllNodes(CallActivity.class);
                            return availableActivities.stream()
                                    .map(activity -> validateCallActivity(availableProcessesIds,
                                                                          bpmnModel.getMainProcess().getId(),
                                                                          activity))
                                    .filter(Optional::isPresent)
                                    .map(Optional::get);
                        }).orElse(Stream.empty());
    }

    private Optional<ModelValidationError> validateCallActivity(Set<String> availableProcessesIds,
                                                                String mainProcess,
                                                                CallActivity callActivity) {
        String calledElement = callActivity.getCalledElement();
        if (isEmpty(calledElement)) {
            return Optional.of(createModelValidationError(format(NO_REFERENCE_FOR_CALL_ACTIVITY_PROBLEM,
                                                                 callActivity.getId(),
                                                                 mainProcess),
                                                          format(NO_REFERENCE_FOR_CALL_ACTIVITY_DESCRIPTION,
                                                                 callActivity.getId(),
                                                                 mainProcess),
                                                          NO_REFERENCE_FOR_CALL_ACTIVITY_REFERENCE_NAME)
            );
        } else {
            String calledElementId = calledElement.replace("process-",
                                                           "");
            return !availableProcessesIds.contains(calledElementId) ?
                    Optional.of(createModelValidationError(INVALID_CALL_ACTIVITY_REFERENCE_PROBLEM,
                                                           format(INVALID_CALL_ACTIVITY_REFERENCE_DESCRIPTION,
                                                                  callActivity.getId(),
                                                                  calledElementId,
                                                                  mainProcess,
                                                                  INVALID_CALL_ACTIVITY_REFERENCE_NAME),
                                                           INVALID_CALL_ACTIVITY_REFERENCE_NAME)) : Optional.empty();
        }
    }
}
