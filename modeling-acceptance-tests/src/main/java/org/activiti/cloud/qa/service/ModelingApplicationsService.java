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

package org.activiti.cloud.qa.service;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.qa.rest.ModelingFeignConfiguration;
import org.activiti.cloud.qa.rest.feign.FeignRestDataClient;

import static org.activiti.cloud.qa.rest.ModelingFeignConfiguration.modelingDecoder;
import static org.activiti.cloud.qa.rest.ModelingFeignConfiguration.modelingEncoder;

/**
 * Modeling groups service
 */
public interface ModelingApplicationsService extends FeignRestDataClient<ModelingApplicationsService, Application> {

    String PATH = "/v1/applications";

    @Override
    default Class<ModelingApplicationsService> getType() {
        return ModelingApplicationsService.class;
    }

    @Override
    default Encoder encoder() {
        return modelingEncoder;
    }

    @Override
    default Decoder decoder() {
        return modelingDecoder;
    }

    static ModelingApplicationsService build(Encoder encoder,
                                             Decoder decoder,
                                             String baseUrl) {
        return FeignRestDataClient
                .builder(encoder,
                         decoder)
                .target(ModelingApplicationsService.class,
                        baseUrl + PATH);
    }
}
