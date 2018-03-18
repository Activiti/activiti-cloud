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
import org.activiti.cloud.qa.model.modeling.Model;
import org.activiti.cloud.qa.model.modeling.Model.ModelType;
import org.activiti.cloud.qa.model.modeling.ModelingContext;
import org.activiti.cloud.qa.rest.feign.EnableModelingFeignContext;
import org.activiti.cloud.qa.service.ModelingModelsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling steps for process models, from models, ...
 */
@EnableModelingFeignContext
public class ModelingModelsSteps extends ModelingContextSteps<Model> {

    @Autowired
    private ModelingModelsService modelingModelsService;

    @Step
    public Resource<Model> create(String modelName,
                                  String modelType) {
        return create(new Model(UUID.randomUUID().toString(),
                                modelName,
                                ModelType.forText(modelType)));
    }

    @Step
    public void editAndSaveCurrentModel() {
        Resource<? extends ModelingContext> currentModel = getCurrentModelingContext();
        assertThat(currentModel.getContent()).isInstanceOf(Model.class);

        Model model = (Model) currentModel.getContent();
        model.setContent("updated content");

        modelingModelsService.updateByUri(currentModel.getLink(REL_SELF).getHref(),
                                          model);
        updateCurrentModelingObject();
    }

    @Step
    public void checkCurrentModelVersion(String expectedModelVersion) {
        Resource<? extends ModelingContext> currentModel = getCurrentModelingContext();
        assertThat(currentModel.getContent()).isInstanceOf(Model.class);

        Model model = (Model) currentModel.getContent();
        assertThat(model.getVersion()).isEqualTo(expectedModelVersion);
    }

    @Override
    protected String getRel() {
        return Model.PROJECT_MODELS_REL;
    }

    @Override
    public ModelingModelsService service() {
        return modelingModelsService;
    }
}
