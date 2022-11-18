/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.modeling.rest.config;

import org.activiti.cloud.services.modeling.rest.controller.ModelController;
import org.activiti.cloud.services.modeling.rest.controller.ModelingRestExceptionHandler;
import org.activiti.cloud.services.modeling.rest.controller.ModelsSchemaController;
import org.activiti.cloud.services.modeling.rest.controller.ProjectController;
import org.activiti.cloud.services.modeling.rest.validation.ValidationControllerAdvice;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(ErrorMvcAutoConfiguration.class)
@PropertySource("classpath:modeling-rest.properties")
@Import(
    {
        ModelController.class,
        ProjectController.class,
        ModelsSchemaController.class,
        ModelingRestExceptionHandler.class,
        ValidationControllerAdvice.class
    }
)
public class RestControllerAutoConfiguration {}
