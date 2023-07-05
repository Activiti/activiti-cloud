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

import static org.springframework.util.StringUtils.isEmpty;

import java.util.Optional;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link BpmnCommonModelValidator} for validating assignee attribute for user tasks
 */
public class BpmnModelUserTaskAssigneeValidator implements BpmnCommonModelValidator {

    private static final String NO_ASSIGNEE_PROBLEM_TITLE = "No assignee for user task";
    private static final String NO_ASSIGNEE_DESCRIPTION =
        "One of the attributes 'assignee','candidateUsers' or" +
        " 'candidateGroups' are mandatory on user task with id: '%s' %s";
    private static final String NO_ASSIGNEE_DESCRIPTION_NAME = "and name: '%s'";
    private static final String USER_TASK_ASSIGNEE_VALIDATOR_NAME = "BPMN user task assignee validator";

    private final FlowElementsExtractor flowElementsExtractor;

    public BpmnModelUserTaskAssigneeValidator(FlowElementsExtractor flowElementsExtractor) {
        this.flowElementsExtractor = flowElementsExtractor;
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext) {
        return flowElementsExtractor
            .extractFlowElements(bpmnModel, UserTask.class)
            .stream()
            .map(this::validateTaskAssignedUser)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private Optional<ModelValidationError> validateTaskAssignedUser(UserTask userTask) {
        if (
            !(
                (
                    isEmpty(userTask.getAssignee()) ||
                    (
                        userTask.getAssignee().equalsIgnoreCase("$initiator") ||
                        userTask.getAssignee().equalsIgnoreCase("${initiator}")
                    )
                ) &&
                CollectionUtils.isEmpty(userTask.getCandidateUsers()) &&
                CollectionUtils.isEmpty(userTask.getCandidateGroups())
            )
        ) {
            return Optional.empty();
        }

        return Optional.of(
            new ModelValidationError(
                NO_ASSIGNEE_PROBLEM_TITLE,
                getNoAssigneeErrorDescription(userTask),
                USER_TASK_ASSIGNEE_VALIDATOR_NAME
            )
        );
    }

    private String getNoAssigneeErrorDescription(UserTask userTask) {
        return String.format(
            NO_ASSIGNEE_DESCRIPTION,
            userTask.getId(),
            (
                StringUtils.hasLength(userTask.getName())
                    ? String.format(NO_ASSIGNEE_DESCRIPTION_NAME, userTask.getName())
                    : "and empty name"
            )
        );
    }
}
