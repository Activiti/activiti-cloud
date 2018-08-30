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

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.activiti.cloud.organization.api.ModelType.FORM;
import static org.activiti.cloud.organization.api.ModelType.PROCESS;

/**
 * Model service configuration
 */
@Configuration
public class ModelServiceConfiguration {

    private final ProcessModelReferenceService processModelService;

    private final FormModelReferenceService formModelService;

    @Autowired
    public ModelServiceConfiguration(ProcessModelReferenceService processModelService,
                                     FormModelReferenceService formModelService) {
        this.processModelService = processModelService;
        this.formModelService = formModelService;
    }

    @Bean
    public Map<ModelType, BaseModelService<ModelReference>> modelServices() {
        Map<ModelType, BaseModelService<ModelReference>> modelServices = new HashMap<>();
        modelServices.put(PROCESS,
                          processModelService);
        modelServices.put(FORM,
                          formModelService);
        return modelServices;
    }
}
