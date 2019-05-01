/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.acc.modeling.modeling;

import org.activiti.cloud.organization.api.Model;

import java.util.Optional;

/**
 * Modeling model identifier
 */
public class ModelIdentifier<M> implements ModelingIdentifier<M> {

    private String modelName;

    private String modelType;

    private String modelVersion;

    public ModelIdentifier(String modelName,
                           String modelType) {
        this(modelName,
             modelType,
             null);
    }

    public ModelIdentifier(String modelName,
                           String modelType,
                           String modelVersion) {
        this.modelName = modelName;
        this.modelType = modelType;
        this.modelVersion = modelVersion;
    }

    @Override
    public boolean test(M modelingContext) {
        return Optional.ofNullable(modelingContext)
                .filter(Model.class::isInstance)
                .map(Model.class::cast)
                .filter(this::testModelName)
                .filter(this::testModelType)
                .filter(this::testModelVersion)
                .isPresent();
    }

    private boolean testModelName(Model model) {
        return modelName.equals(model.getName());
    }

    private boolean testModelType(Model model) {
        return modelType.equalsIgnoreCase(model.getType());
    }

    private boolean testModelVersion(Model model) {
        return modelVersion == null || modelVersion.equals(model.getVersion());
    }

    public static ModelIdentifier identified(String modelName,
                                             String modelType) {
        return new ModelIdentifier(modelName,
                                   modelType);
    }

    public static ModelIdentifier identified(String modelName,
                                             String modelType,
                                             String modelVersion) {
        return new ModelIdentifier(modelName,
                                   modelType,
                                   modelVersion);
    }
}
