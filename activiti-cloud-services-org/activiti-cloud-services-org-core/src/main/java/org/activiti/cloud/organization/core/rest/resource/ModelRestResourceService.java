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

package org.activiti.cloud.organization.core.rest.resource;

import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation for {@link RestResourceService} for the rest resource associated with a model
 */
@Service
public class ModelRestResourceService extends RestResourceService<ModelReference, ModelType, String> {

    private final ModelService modelService;

    @Autowired
    public ModelRestResourceService(ModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    protected ModelReference getResource(ModelType resourceKey,
                                         String resourceId) {
        return modelService.getResource(resourceKey,
                                        resourceId);
    }

    @Override
    protected void createResource(ModelType resourceKey,
                                  ModelReference resource) {
        modelService.createResource(resourceKey,
                                    resource);
    }

    @Override
    protected void updateResource(ModelType resourceKey,
                                  String resourceId,
                                  ModelReference resource) {
        modelService.updateResource(resourceKey,
                                    resourceId,
                                    resource);
    }
}
