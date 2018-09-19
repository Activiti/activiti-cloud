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

import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Model service configuration
 */
@Configuration
public class ModelServiceConfiguration {

    private final ProcessModelType processModelType;

    private final ProcessModelReferenceService processModelService;

    @Autowired
    public ModelServiceConfiguration(ProcessModelType processModelType,
                                     ProcessModelReferenceService processModelService) {
        this.processModelType = processModelType;
        this.processModelService = processModelService;
    }

    @Bean
    public Map<String, BaseModelService<ModelReference>> modelServices() {
        Map<String, BaseModelService<ModelReference>> modelServices = new HashMap<>();
        modelServices.put(processModelType.getName(),
                          processModelService);
        return modelServices;
    }
}
