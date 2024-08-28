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
package org.activiti.cloud.conf;

import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.rest.ApplicationAdminController;
import org.activiti.cloud.services.query.rest.ApplicationController;
import org.activiti.cloud.services.query.rest.CommonExceptionHandlerQuery;
import org.activiti.cloud.services.query.rest.ProcessDefinitionAdminController;
import org.activiti.cloud.services.query.rest.ProcessDefinitionController;
import org.activiti.cloud.services.query.rest.ProcessInstanceAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceController;
import org.activiti.cloud.services.query.rest.ProcessInstanceDeleteController;
import org.activiti.cloud.services.query.rest.ProcessInstanceDiagramAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceDiagramController;
import org.activiti.cloud.services.query.rest.ProcessInstanceServiceTasksAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceTasksAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceTasksController;
import org.activiti.cloud.services.query.rest.ProcessInstanceVariableAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceVariableController;
import org.activiti.cloud.services.query.rest.ProcessModelAdminController;
import org.activiti.cloud.services.query.rest.ProcessModelController;
import org.activiti.cloud.services.query.rest.ServiceTaskAdminController;
import org.activiti.cloud.services.query.rest.ServiceTaskIntegrationContextAdminController;
import org.activiti.cloud.services.query.rest.TaskAdminController;
import org.activiti.cloud.services.query.rest.TaskController;
import org.activiti.cloud.services.query.rest.TaskDeleteController;
import org.activiti.cloud.services.query.rest.TaskVariableAdminController;
import org.activiti.cloud.services.query.rest.TaskVariableController;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@AutoConfiguration
@Import(
    {
        CommonExceptionHandlerQuery.class,
        ProcessDefinitionAdminController.class,
        ProcessDefinitionController.class,
        ProcessInstanceAdminController.class,
        ProcessInstanceController.class,
        ProcessInstanceDeleteController.class,
        ProcessInstanceDiagramAdminController.class,
        ProcessInstanceDiagramController.class,
        ProcessInstanceTasksAdminController.class,
        ProcessInstanceTasksController.class,
        ProcessInstanceVariableAdminController.class,
        ProcessInstanceVariableController.class,
        ProcessModelAdminController.class,
        ProcessModelController.class,
        TaskAdminController.class,
        TaskController.class,
        TaskDeleteController.class,
        TaskVariableAdminController.class,
        TaskVariableController.class,
        ServiceTaskAdminController.class,
        ProcessInstanceServiceTasksAdminController.class,
        ServiceTaskIntegrationContextAdminController.class,
        ApplicationController.class,
        ApplicationAdminController.class,
    }
)
@PropertySource("classpath:query-rest.properties")
public class QueryRestControllersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProcessDiagramGenerator processDiagramGenerator() {
        return new DefaultProcessDiagramGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessDiagramGeneratorWrapper processDiagramGeneratorWrapper(
        ProcessDiagramGenerator processDiagramGenerator
    ) {
        return new ProcessDiagramGeneratorWrapper(processDiagramGenerator);
    }

    @Bean
    @ConditionalOnMissingBean
    RepositoryRestConfigurer dataRestRepositoryRestConfigurer() {
        return new RepositoryRestConfigurer() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
                config.disableDefaultExposure();
            }
        };
    }
}
