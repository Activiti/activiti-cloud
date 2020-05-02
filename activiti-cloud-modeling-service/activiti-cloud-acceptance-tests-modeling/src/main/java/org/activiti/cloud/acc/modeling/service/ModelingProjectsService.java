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

package org.activiti.cloud.acc.modeling.service;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.jackson.JacksonEncoder;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.Project;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;

import java.io.File;

import static org.activiti.cloud.acc.modeling.rest.ModelingFeignConfiguration.modelingDecoder;
import static org.activiti.cloud.acc.modeling.rest.ModelingFeignConfiguration.modelingEncoder;

/**
 * Modeling groups service
 */
public interface ModelingProjectsService extends FeignRestDataClient<ModelingProjectsService, Project> {

    String PATH = "/v1/projects";

    @RequestLine("GET ?name={name}")
    @Headers("Content-Type: application/json")
    PagedModel<EntityModel<Project>> findAllByName(@Param("name") String name);

    @RequestLine("GET")
    @Headers("Content-Type: application/json")
    Response exportProject();

    @RequestLine("GET")
    @Headers("Content-Type: application/json")
    Response validateProject();

    @RequestLine("POST")
    @Headers("Content-Type: multipart/form-data")
    EntityModel<Model> importProjectModel(@Param("file") File file);

    @Override
    default Class<ModelingProjectsService> getType() {
        return ModelingProjectsService.class;
    }

    default Response exportProjectByUri(String uri) {
        return FeignRestDataClient
                .builder(new FormEncoder(new JacksonEncoder()),
                         new Decoder.Default())
                .target(getType(),
                        uri)
                .exportProject();
    }

    default Response validateProjectByUri(String uri) {
        return FeignRestDataClient
                .builder(new FormEncoder(new JacksonEncoder()),
                        new Decoder.Default())
                .target(getType(),
                        uri)
                .validateProject();
    }

    default EntityModel<Model> importProjectModelByUri(String uri,
                                                    File file) {
        return FeignRestDataClient
                .builder(new FormEncoder(new JacksonEncoder()),
                         modelingDecoder)
                .target(getType(),
                        uri)
                .importProjectModel(file);
    }

    @Override
    default Encoder encoder() {
        return modelingEncoder;
    }

    @Override
    default Decoder decoder() {
        return modelingDecoder;
    }

    static ModelingProjectsService build(Encoder encoder,
                                         Decoder decoder,
                                         String baseUrl) {
        return FeignRestDataClient
                .builder(encoder,
                         decoder)
                .target(ModelingProjectsService.class,
                        baseUrl + PATH);
    }
}
