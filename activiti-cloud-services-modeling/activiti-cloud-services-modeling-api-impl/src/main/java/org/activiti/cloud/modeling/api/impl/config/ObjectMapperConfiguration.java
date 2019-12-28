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

package org.activiti.cloud.modeling.api.impl.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.impl.ProjectImpl;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(JacksonAutoConfiguration.class)
public class ObjectMapperConfiguration {

  @Bean
  public Module jsonModelingModuleImpl() {
    SimpleModule module = new SimpleModule("jsonModelingModuleImpl",
      Version.unknownVersion());
    SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();

    resolver.addMapping(Project.class,
      ProjectImpl.class);
    resolver.addMapping(Model.class,
      ModelImpl.class);

    module.setAbstractTypes(resolver);
    return module;
  }
}
