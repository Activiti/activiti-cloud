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

package org.activiti.cloud.qa.story;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.model.modeling.Model;
import org.activiti.cloud.qa.steps.ModelingModelsSteps;
import org.activiti.cloud.qa.steps.ModelingProjectsSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.qa.model.modeling.ModelIdentifier.identified;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.named;

/**
 * Modeling groups scenarios
 */
public class ModelingModels {

    @Steps
    private ModelingProjectsSteps modelingProjectsSteps;

    @Steps
    private ModelingModelsSteps modelingModelsSteps;

    @When("creates the $modelType model '$modelName'")
    public void createModel(String modelType,
                            String modelName) {
        Resource<Model> createdModel = modelingModelsSteps.create(modelName,
                                                                  modelType);
        modelingProjectsSteps.addToCurrentContext(createdModel);
    }

    @Then("the $modelType model '$modelName' is created")
    public void checkModelExistsInCurrentProject(String modelType,
                                                 String modelName) {
        modelingModelsSteps.checkExistsInCurrentContext(identified(modelName,
                                                                   modelType));
    }

    @Then("the version of the $modelType model '$modelName' is $modelVersion")
    public void checkModelVersionInCurrentProject(String modelType,
                                                  String modelName,
                                                  String modelVersion) {
        modelingModelsSteps.checkExistsInCurrentContext(identified(modelName,
                                                                     modelType,
                                                                     modelVersion));
    }

    @Then("delete model '$modelName'")
    public void deleteModel(String modelName) {
        modelingModelsSteps.deleteAll(named(modelName));
    }

    @Given("an existing $modelType model '$modelName' with version $modelVersion")
    public void ensureModelVersionExists(String modelType,
                                         String modelName,
                                         String modelVersion) {
        if (!modelingModelsSteps.exists(identified(modelName,
                                                   modelType,
                                                   modelVersion))) {
            modelingModelsSteps.create(modelName,
                                       modelType);
        }
    }

    @When("the user opens the $modelType model '$modelName'")
    public void openModel(String modelType,
                          String modelName) {
        modelingModelsSteps.openModelingObject(identified(modelName,
                                                          modelType));
    }

    @When("edits and saves the model")
    public void editAndSaveCurrentModel() {
        modelingModelsSteps.editAndSaveCurrentModel();
    }

    @Then("the model is saved with the version $modelVersion")
    public void checkCurrentModelVersion(String modelVersion) {
        modelingModelsSteps.checkCurrentModelVersion(modelVersion);
    }
}
