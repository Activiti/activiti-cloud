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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ValidationContext;

/**
 * Implementation of {@link ValidationContext} in project validation context
 */
public class ProjectValidationContext implements ValidationContext {

    private final List<Model> availableModels;

    public ProjectValidationContext(List<Model> availableModels) {
        this.availableModels = availableModels;
    }

    public ProjectValidationContext(Model... availableModels) {
        this.availableModels = Arrays.asList(availableModels);
    }

    @Override
    public List<Model> getAvailableModels(ModelType modelType) {
        return availableModels
                .stream()
                .filter(model -> modelType.getName().equals(model.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return availableModels.isEmpty();
    }
}
