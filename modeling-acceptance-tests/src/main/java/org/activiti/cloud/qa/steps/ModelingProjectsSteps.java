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

package org.activiti.cloud.qa.steps;

import java.util.UUID;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.qa.model.modeling.Project;
import org.activiti.cloud.qa.rest.ModelingFeignConfiguration;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.ModelingProjectsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.test.context.ContextConfiguration;

/**
 * Modeling groups steps
 */
@EnableFeignContext
@ContextConfiguration(classes = {ModelingTestsConfigurationProperties.class, ModelingFeignConfiguration.class})
public class ModelingProjectsSteps extends ModelingContextSteps<Project> {

    @Autowired
    private ModelingProjectsService modelingProjectsService;

    @Step
    public Resource<Project> create(String projectName) {
        return create(new Project(UUID.randomUUID().toString(),
                                  projectName));
    }

    @Override
    protected String getRel() {
        return Project.GROUP_PROJECTS_REL;
    }

    @Override
    public ModelingProjectsService service() {
        return modelingProjectsService;
    }
}
