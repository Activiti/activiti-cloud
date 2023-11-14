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
package org.activiti.cloud.services.modeling.rest.validation;

import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.modeling.validation.project.ProjectNameValidator;
import org.springframework.validation.Errors;

/**
 * Validator fot project payload
 */
public class ProjectPayloadValidator extends GenericPayloadValidator<Project> {

    private ProjectNameValidator projectNameValidator;

    public ProjectPayloadValidator(boolean validateRequiredFields, ProjectNameValidator projectNameValidator) {
        super(Project.class, validateRequiredFields);
        this.projectNameValidator = projectNameValidator;
    }

    @Override
    public void validatePayload(Project project, Errors errors) {
        if (validateRequiredFields || project.getName() != null) {
            projectNameValidator
                .validateName(project.getName(), "project")
                .forEach(error -> errors.rejectValue("name", error.getErrorCode(), error.getDescription()));
        }
    }
}
