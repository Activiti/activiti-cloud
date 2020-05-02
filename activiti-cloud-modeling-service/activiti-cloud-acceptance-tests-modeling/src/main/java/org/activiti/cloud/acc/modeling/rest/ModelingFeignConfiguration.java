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

package org.activiti.cloud.acc.modeling.rest;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.activiti.cloud.acc.modeling.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.acc.modeling.service.ModelingModelsService;
import org.activiti.cloud.acc.modeling.service.ModelingProjectsService;
import org.activiti.cloud.acc.shared.rest.feign.FeignConfiguration;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.acc.shared.service.SwaggerService;
import org.activiti.cloud.modeling.api.impl.config.ObjectMapperConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;

/**
 * Feign Configuration
 */
@Configuration
@Import({
  ObjectMapperConfiguration.class,
  JacksonAutoConfiguration.class,
  FeignConfiguration.class
})
public class ModelingFeignConfiguration {

  @Autowired
  private ModelingTestsConfigurationProperties modelingTestsConfigurationProperties;

  public static Encoder modelingEncoder;

  public static Decoder modelingDecoder;

  @Bean
  public Module jackson2HalModule() {
    return new Jackson2HalModule();
  }

  @Bean
  public Encoder modelingEncoder(ObjectMapper objectMapper) {
    return modelingEncoder = new JacksonEncoder(objectMapper);
  }

  @Bean
  public Decoder modelingDecoder(ObjectMapper objectMapper) {
    return modelingDecoder = new JacksonDecoder(objectMapper);
  }

  @Bean
  public ModelingProjectsService modelingApplicationsService(Encoder modelingEncoder,
                                                             Decoder modelingDecoder) {
    return ModelingProjectsService
      .build(modelingEncoder,
        modelingDecoder,
        modelingTestsConfigurationProperties.getModelingUrl());
  }

  @Bean
  public ModelingModelsService modelingModelsService(Encoder modelingEncoder,
                                                     Decoder modelingDecoder) {
    return ModelingModelsService
      .build(modelingEncoder,
        modelingDecoder,
        modelingTestsConfigurationProperties.getModelingUrl());
  }

  @Bean
  public SwaggerService modelingSwaggerService(){
    return FeignRestDataClient
      .builder(new feign.codec.Encoder.Default(),
        new feign.codec.Decoder.Default())
      .target(SwaggerService.class, modelingTestsConfigurationProperties.getModelingUrl());
  }
}
