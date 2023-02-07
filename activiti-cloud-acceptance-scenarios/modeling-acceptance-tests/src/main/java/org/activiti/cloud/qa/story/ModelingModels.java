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
package org.activiti.cloud.qa.story;

import static org.activiti.cloud.acc.modeling.modeling.ModelIdentifier.identified;
import static org.activiti.cloud.acc.modeling.modeling.ModelingNamingIdentifier.modelNamed;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.modeling.steps.ModelingModelsSteps;
import org.activiti.cloud.acc.modeling.steps.ModelingProjectsSteps;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Modeling models scenarios
 */
public class ModelingModels {

    @Steps
    private ModelingProjectsSteps modelingProjectSteps;

    @Steps
    private ModelingModelsSteps modelingModelsSteps;

    @When("creates the $modelType model '$modelName'")
    public void createModel(String modelType, String modelName) {
        createModel(modelType, modelName, null);
    }

    @When("creates the $modelType model $modelName with process variables '$processVariables'")
    public void createModel(String modelType, String modelName, List<String> processVariables) {
        modelingModelsSteps.create(modelName, modelType, processVariables);
    }

    @Then("the $modelType model '$modelName' is created")
    public void checkModelExistsInCurrentProject(String modelType, String modelName) {
        modelingModelsSteps.checkExistsInCurrentContext(identified(modelName, modelType));
    }

    @Then("the version of the $modelType model '$modelName' is $modelVersion")
    public void checkModelVersionInCurrentProject(String modelType, String modelName, String modelVersion) {
        modelingModelsSteps.checkExistsInCurrentContext(identified(modelName, modelType, modelVersion));
    }

    @Then("it contains process variables '$procressVariables'")
    @Alias("the model is saved with the process variables '$procressVariables'")
    public void checkCurrentModelContainsVariables(List<String> processVariables) {
        modelingModelsSteps.checkCurrentModelContainsVariables(processVariables.toArray(new String[0]));
    }

    @Then("delete model '$modelName'")
    public void deleteModel(String modelName) {
        modelingModelsSteps.deleteAll(modelNamed(modelName));
    }

    @When("opens the $modelType model '$modelName'")
    public void openModel(String modelType, String modelName) {
        modelingModelsSteps.openModelingObject(identified(modelName, modelType));
    }

    @When("edits and saves the model")
    public void editAndSaveCurrentModel() {
        modelingModelsSteps.saveCurrentModel(true);
    }

    @When("saves the model")
    public void saveCurrentModel() {
        modelingModelsSteps.saveCurrentModel(false);
    }

    @Then("the model is saved with the version $modelVersion")
    public void checkCurrentModelVersion(String modelVersion) {
        modelingModelsSteps.checkCurrentModelVersion(modelVersion);
    }

    @When("removes the process variable '$processVariable'")
    public void removeProcessVariableInCurrentModel(String processVariable) {
        modelingModelsSteps.removeProcessVariableInCurrentModel(processVariable);
    }

    @When("adds the process variable '$processVariable'")
    public void addProcessVariableInCurrentModela(String processVariable) {
        modelingModelsSteps.addProcessVariableInCurrentModel(Collections.singletonList(processVariable));
    }

    @Then("the model is valid")
    public void validateModelFile() throws IOException {
        modelingModelsSteps.checkCurrentModelValidation();
    }

    @Then("$numberOfErrors validation errors are shown for extensions")
    public void checkCurrentModelValidationErrorsForExtensions(String numberOfErrors) throws IOException {
        modelingModelsSteps.checkCurrentModelValidationFailureForExtensions(
            "#: " + numberOfErrors + " schema violations found"
        );
    }

    @Then("$propertySchema not valid find in extensions")
    public void checkCurrentModelValidationSchemaMatchesForExtensions(String propertySchema) throws IOException {
        modelingModelsSteps.checkCurrentModelValidationFailureForExtensions(
            "#/extensions: extraneous key [" + propertySchema + "] is not permitted"
        );
    }
}
