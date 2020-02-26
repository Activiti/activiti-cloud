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

package org.activiti.cloud.services.modeling.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContent;
import org.activiti.cloud.modeling.api.ModelContentConverter;
import org.activiti.cloud.modeling.api.ModelContentValidator;

/**
 * Service for managing {@link ModelContentValidator}
 */
public class ModelContentService {

    private final Map<String, List<ModelContentValidator>> modelContentValidatorsMapByModelType;

    private final Map<String, ModelContentConverter<? extends ModelContent>> modelContentConvertersMapByModelType;

    private final Map<String, List<ContentUpdateListener>> contentUpdateListenersMapByModelType;

    public ModelContentService(Set<ModelContentValidator> modelValidators,
                               Set<ModelContentConverter<? extends ModelContent>> modelConverters,
                               Set<ContentUpdateListener> contentUpdateListeners) {
        this.modelContentValidatorsMapByModelType = modelValidators
                .stream()
                .collect(Collectors.groupingBy(validator -> validator.getHandledModelType().getName()));

        this.modelContentConvertersMapByModelType = modelConverters
                .stream()
                .collect(Collectors.toMap(converter -> converter.getHandledModelType().getName(),
                                          Function.identity()));
        this.contentUpdateListenersMapByModelType = contentUpdateListeners
                .stream()
                .collect(Collectors.groupingBy(contentUpdateListener -> contentUpdateListener.getHandledModelType().getName()));
    }

    public List<ModelContentValidator> findModelValidators(String modelType) {
        return modelContentValidatorsMapByModelType.get(modelType);
    }

    public Optional<ModelContentConverter<? extends ModelContent>> findModelContentConverter(String modelType) {
        return Optional.ofNullable(modelContentConvertersMapByModelType.get(modelType));
    }

    public List<ContentUpdateListener> findContentUploadListeners(String modelType) {
        return contentUpdateListenersMapByModelType.get(modelType);
    }

    public String getModelContentId(Model model) {
        return String.join("-",
                           model.getType().toLowerCase(),
                           model.getId());
    }

}
