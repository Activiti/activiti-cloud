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

package org.activiti.cloud.services.modeling.service.decorators;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;

public class ModelExtensionsImportDecoratorService {

    private final Map<String, ModelExtensionsImportDecorator> modelExtensionsImportDecorators;
    private final DefaultModelExtensionsImportDecorator defaultModelExtensionsImportDecorator;

    public ModelExtensionsImportDecoratorService(
        List<ModelExtensionsImportDecorator> modelExtensionsImportDecorators,
        DefaultModelExtensionsImportDecorator defaultModelExtensionsImportDecorator
    ) {
        this.modelExtensionsImportDecorators =
            modelExtensionsImportDecorators
                .stream()
                .filter(decorator -> decorator.getHandledModelTypes() != null)
                .filter(decorator -> !decorator.getHandledModelTypes().isEmpty())
                .flatMap(decorator ->
                    decorator
                        .getHandledModelTypes()
                        .stream()
                        .map(ModelType::getName)
                        .map(modelType -> new AbstractMap.SimpleImmutableEntry<>(modelType, decorator))
                )
                .collect(
                    Collectors.toMap(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        AbstractMap.SimpleImmutableEntry::getValue
                    )
                );
        this.defaultModelExtensionsImportDecorator = defaultModelExtensionsImportDecorator;
    }

    public void decorate(Model model, Map<String, Object> extensions) {
        modelExtensionsImportDecorators
            .getOrDefault(model.getType(), defaultModelExtensionsImportDecorator)
            .decorate(model, extensions);
    }
}
