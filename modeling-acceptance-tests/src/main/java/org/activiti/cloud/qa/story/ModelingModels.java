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

import java.util.Arrays;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.qa.steps.ModelingApplicationsSteps;
import org.activiti.cloud.qa.steps.ModelingModelsSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.qa.model.modeling.ModelIdentifier.identified;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.modelNamed;

/**
 * Modeling models scenarios
 */
public class ModelingModels {

    @Steps
    private ModelingApplicationsSteps modelingApplicationsSteps;

    @Steps
    private ModelingModelsSteps modelingModelsSteps;

    @When("creates the $modelType model '$modelName'")
    public void createModel(String modelType,
                            String modelName) {
        modelingModelsSteps.create(modelName,
                                   toModelType(modelType));
    }

    @Then("the $modelType model '$modelName' is created")
    public void checkModelExistsInCurrentApplication(String modelType,
                                                     String modelName) {
        modelingModelsSteps.checkExistsInCurrentContext(identified(modelName,
                                                                   toModelType(modelType)));
    }

    @Then("the version of the $modelType model '$modelName' is $modelVersion")
    public void checkModelVersionInCurrentApplication(String modelType,
                                                      String modelName,
                                                      String modelVersion) {
        modelingModelsSteps.checkExistsInCurrentContext(identified(modelName,
                                                                   toModelType(modelType),
                                                                   modelVersion));
    }

    @Then("delete model '$modelName'")
    public void deleteModel(String modelName) {
        modelingModelsSteps.deleteAll(modelNamed(modelName));
    }

    @Given("an existing $modelType model '$modelName' with version $modelVersion")
    public void ensureModelVersionExists(String modelType,
                                         String modelName,
                                         String modelVersion) {
        if (!modelingModelsSteps.exists(identified(modelName,
                                                   toModelType(modelType),
                                                   modelVersion))) {
            modelingModelsSteps.create(modelName,
                                       toModelType(modelType));
        }
    }

    @When("the user opens the $modelType model '$modelName'")
    public void openModel(String modelType,
                          String modelName) {
        modelingModelsSteps.openModelingObject(identified(modelName,
                                                          toModelType(modelType)));
    }

    @When("edits and saves the model")
    public void editAndSaveCurrentModel() {
        modelingModelsSteps.editAndSaveCurrentModel();
    }

    @Then("the model is saved with the version $modelVersion")
    public void checkCurrentModelVersion(String modelVersion) {
        modelingModelsSteps.checkCurrentModelVersion(modelVersion);
    }

    protected ModelType toModelType(String modelType) {
        return Arrays.asList(ModelType.values())
                .stream()
                .filter(type -> type.name().equalsIgnoreCase(modelType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown model type: " + modelType));
    }
}
