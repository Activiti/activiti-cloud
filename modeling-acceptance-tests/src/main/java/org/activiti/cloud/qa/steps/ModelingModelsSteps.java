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

import java.util.Optional;
import java.util.UUID;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.qa.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.qa.model.modeling.EnableModelingContext;
import org.activiti.cloud.qa.service.ModelingModelsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling steps for process models, from models, ...
 */
@EnableModelingContext
public class ModelingModelsSteps extends ModelingContextSteps<Model> {

    public static final String PROJECT_MODELS_REL = "models";

    @Autowired
    private ModelingModelsService modelingModelsService;

    @Autowired
    private ModelingTestsConfigurationProperties config;

    @Step
    public Resource<Model> create(String modelName,
                                  String modelType) {
        String id = UUID.randomUUID().toString();
        Model model = mock(Model.class);
        doReturn(id).when(model).getId();
        doReturn(modelType.toUpperCase()).when(model).getType();
        doReturn(modelName).when(model).getName();
        return create(id,
                      model);
    }

    @Step
    public void editAndSaveCurrentModel() {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        assertThat(currentContext.getContent()).isInstanceOf(Model.class);

        Model model = currentContext.getContent();
        model.setContent("updated content");

        modelingModelsService.updateByUri(currentContext.getLink(REL_SELF).getHref().replace("http://activiti-cloud-modeling-backend", config.getModelingUrl()),
                                          model);
        updateCurrentModelingObject();
    }

    @Step
    public void checkCurrentModelVersion(String expectedModelVersion) {
        Resource<Model> currentContext = checkAndGetCurrentContext(Model.class);
        Model model = currentContext.getContent();
        assertThat(model.getVersion()).isEqualTo(expectedModelVersion);
    }

    @Override
    protected Optional<String> getRel() {
        return Optional.of(PROJECT_MODELS_REL);
    }

    @Override
    public ModelingModelsService service() {
        return modelingModelsService;
    }
}
