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

package org.activiti.cloud.organization.core.rest.client.feign;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import org.activiti.cloud.organization.core.model.Model.ModelType;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.activiti.cloud.organization.core.service.ValidationErrorRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

/**
 * Feign model rest client services handler
 */
@Service
@ConditionalOnClass(FeignClient.class)
public class FeignModelService implements ModelService {

    @Resource
    private Map<ModelType, BaseModelService<ModelReference>> modelServices;

    @Override
    public ModelReference getResource(ModelType modelType,
                                      String modelId) {
        return modelServices.get(modelType).getResource(modelId);
    }

    @Override
    public void createResource(ModelType modelType,
                               ModelReference model) {
        modelServices.get(modelType).createResource(model);
    }

    @Override
    public void updateResource(ModelType modelType,
                               String id,
                               ModelReference model) {
        modelServices.get(modelType).updateResource(id,
                                                    model);
    }

    @Override
    public void deleteResource(ModelType modelType,
                               String id) {
        modelServices.get(modelType).deleteResource(id);
    }

    @Override
    public List<ValidationErrorRepresentation> validateResourceContent(ModelType modelType,
                                                                       byte[] file) {
        return modelServices.get(modelType).validateResourceContent(file);
    }
}
