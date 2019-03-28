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

package org.activiti.cloud.services.organization.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelContent;
import org.activiti.cloud.organization.api.ModelContentConverter;
import org.activiti.cloud.organization.api.ModelValidator;
import org.springframework.stereotype.Service;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;

/**
 * Service for managing {@link ModelValidator}
 */
@Service
public class ModelContentService {

    private final Map<String, ModelValidator> modelContentValidatorsMapByModelType;

    private final Map<String, ModelValidator> modelJsonValidatorsMapByModelType;

    private final Map<String, ModelContentConverter<? extends ModelContent>> modelContentConvertersMapByModelType;

    public ModelContentService(ModelTypeService modelTypeService,
                               Set<ModelValidator> modelValidators,
                               Set<ModelContentConverter<? extends ModelContent>> modelConverters) {
        this.modelContentValidatorsMapByModelType = modelValidators
                .stream()
                .filter(validator -> !CONTENT_TYPE_JSON.equals(validator.getHandledContentType()) ||
                        modelTypeService.isJson(validator.getHandledModelType()))
                .collect(Collectors.toMap(validator -> validator.getHandledModelType().getName(),
                                          Function.identity()));

        this.modelJsonValidatorsMapByModelType = modelValidators
                .stream()
                .filter(validator -> CONTENT_TYPE_JSON.equals(validator.getHandledContentType()))
                .collect(Collectors.toMap(validator -> validator.getHandledModelType().getName(),
                                          Function.identity()));

        this.modelContentConvertersMapByModelType = modelConverters
                .stream()
                .collect(Collectors.toMap(converter -> converter.getHandledModelType().getName(),
                                          Function.identity()));
    }

    public Optional<ModelValidator> findModelValidator(String modelType,
                                                       String contentType) {
        return Optional.ofNullable(CONTENT_TYPE_JSON.equals(contentType) ?
                                           modelJsonValidatorsMapByModelType.get(modelType) :
                                           modelContentValidatorsMapByModelType.get(modelType));
    }

    public Optional<ModelContentConverter<? extends ModelContent>> findModelContentConverter(String modelType) {
        return Optional.ofNullable(modelContentConvertersMapByModelType.get(modelType));
    }

    public String getModelContentId(Model model) {
        return String.join("-",
                           model.getType().toLowerCase(),
                           model.getId());
    }

}
