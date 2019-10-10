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

package org.activiti.cloud.services.organization.validation.project;

import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.ValidationContext;

import java.util.stream.Stream;

/**
 * Implementation of {@link ProjectValidator} for validating that a project has at least one project.
 */
public class ProjectConsistencyValidator implements ProjectValidator {

    private final String EMPTY_PROJECT_PROBLEM = "Invalid project";
    private final String EMPTY_PROJECT_DESCRIPTION = "Project must contain at least one process";

    private final ProcessModelType processModelType;

    public ProjectConsistencyValidator(ProcessModelType processModelType) {
        this.processModelType = processModelType;
    }

    @Override
    public Stream<ModelValidationError> validate(Project project,
                                                 ValidationContext validationContext) {
        return validationContext.getAvailableModels(processModelType)
                .stream()
                .findFirst()
                .map(model -> Stream.<ModelValidationError>empty())
                .orElseGet(() -> Stream.of(createModelValidationError(EMPTY_PROJECT_PROBLEM,
                                                                      EMPTY_PROJECT_DESCRIPTION)));
    }
}
