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
import static org.activiti.cloud.acc.modeling.modeling.ModelingNamingIdentifier.projectNamed;
import static org.activiti.cloud.acc.modeling.modeling.ModelingNamingIdentifier.projectsNamed;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsFile;

import java.io.IOException;
import java.util.List;
import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.modeling.steps.ModelingModelsSteps;
import org.activiti.cloud.acc.modeling.steps.ModelingProjectsSteps;
import org.activiti.cloud.modeling.api.Model;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.EntityModel;

/**
 * Modeling projects scenarios
 */
public class ModelingProjects {

    @Steps
    private ModelingProjectsSteps modelingProjectsSteps;

    @Steps
    private ModelingModelsSteps modelingModelsSteps;

    @When("the user creates a project '$projectName'")
    public void createProject(String projectName) {
        modelingProjectsSteps.create(projectName);
    }

    @When("update the project name to '$projectName'")
    public void updateProjectName(String projectName) {
        modelingProjectsSteps.updateProjectName(projectName);
    }

    @Then("the project name is updated to '$projectName'")
    public void checkProjectName(String projectName) {
        modelingProjectsSteps.checkCurrentProjectName(projectName);
    }

    @Then("the project '$projectName' is created")
    public void checkProjectExists(String projectName) {
        modelingProjectsSteps.checkExists(projectNamed(projectName));
    }

    @When("the user gets the projects by name '$name'")
    public void findProjectsByName(String name) {
        modelingProjectsSteps.findByName(name);
    }

    @Then("the retrieved projects are '$projectNames'")
    public void checkCurrentProjects(List<String> projectNames) {
        modelingProjectsSteps.checkCurrentProjects(projectNames);
    }

    @Given("existing projects '$projectNames'")
    public void ensureProjectsExist(List<String> projectNames) {
        projectNames.forEach(this::ensureProjectExists);
    }

    @Then("delete project '$projectName'")
    @When("the user delete the project '$projectName'")
    public void deleteProject(String projectName) {
        modelingProjectsSteps.deleteAll(projectNamed(projectName));
    }

    @Then("delete projects '$projectNames'")
    public void deleteProjects(List<String> projectNames) {
        modelingProjectsSteps.deleteAll(projectsNamed(projectNames));
    }

    @Given("an existing project '$projectName'")
    public void ensureProjectExists(String projectName) {
        if (!modelingProjectsSteps.exists(projectNamed(projectName))) {
            modelingProjectsSteps.create(projectName);
        }
    }

    @When("the user opens the project '$projectName'")
    public void openProject(String projectName) {
        modelingProjectsSteps.openModelingObject(projectNamed(projectName));
    }

    @When("the user opens the existing project '$projectName'")
    public void openExistingProject(String projectName) {
        ensureProjectExists(projectName);
        openProject(projectName);
    }

    @Then("the project '$projectName' is deleted")
    public void checkProjectNotFound(String projectName) {
        modelingProjectsSteps.checkProjectNotFound(projectNamed(projectName));
    }

    @When("the user export the project")
    public void exportProject() throws IOException {
        modelingProjectsSteps.checkCurrentProjectExport();
    }

    @Given("an project '$projectName' with $modelType model '$modelName'")
    public void ensureProjectWithModelExists(String projectName, String modelType, String modelName) {
        ensureProjectWithModelExists(projectName, modelType, modelName, null);
    }

    @Given("an project '$projectName' with $modelType model $modelName and process variables '$processVariables'")
    public void ensureProjectWithModelWithVariablesExists(
        String projectName,
        String modelType,
        String modelName,
        List<String> processVariables
    ) {
        ensureProjectWithModelExists(projectName, modelType, modelName, null, processVariables);
    }

    @Given("an project '$projectName' with $modelType model '$modelName' version $modelVersion")
    public void ensureProjectWithModelExists(
        String projectName,
        String modelType,
        String modelName,
        String modelVersion
    ) {
        ensureProjectWithModelExists(projectName, modelType, modelName, modelVersion, null);
    }

    public void ensureProjectWithModelExists(
        String projectName,
        String modelType,
        String modelName,
        String modelVersion,
        List<String> processVariables
    ) {
        ensureProjectExists(projectName);
        openProject(projectName);
        if (!modelingModelsSteps.existsInCurrentContext(identified(modelName, modelType, modelVersion))) {
            resourceAsFile(
                modelType +
                "/" +
                setExtension(modelName, modelingModelsSteps.getModelType(modelType).getContentFileExtension())
            )
                .map(file -> {
                    EntityModel<Model> model = modelingProjectsSteps.importModelInCurrentProject(file);
                    if (processVariables != null) {
                        modelingModelsSteps.addProcessVariableToModelModel(model.getContent(), processVariables);
                        modelingModelsSteps.saveModel(model);
                    }
                    return model;
                })
                .orElseGet(() -> modelingModelsSteps.create(modelName, modelType, processVariables));
        }
    }

    @Then("the exported project contains the $modelType model $modelName with process variables '$processVariables'")
    public void checkExportedProjectContainsModel(String modelType, String modelName, List<String> processVariables) {
        modelingProjectsSteps.checkExportedProjectContainsModel(
            modelingProjectsSteps.getModelType(modelType),
            modelName,
            processVariables
        );
    }

    @Then("the project can be exported due to validation errors")
    public void checkCurrentProjectExportNotFailsOnValidation() throws IOException {
        modelingProjectsSteps.checkCurrentProjectExportNotFail("Validation errors found in project's models");
    }

    @When("the user validate the project")
    public void validateProject() throws IOException {
        modelingProjectsSteps.checkCurrentProjectValidate();
    }

    @Then("the project should contain validation errors")
    public void checkCurrentProjectExportFailsOnValidation() throws IOException {
        modelingProjectsSteps.checkCurrentProjectValidationFails("Validation errors found in project's models");
    }

    @Then("the project should have validation errors with message $errorMessage")
    public void checkCurrentProjectExportFailsOnValidation(String errorMessage) throws IOException {
        modelingProjectsSteps.checkCurrentProjectValidationFails(errorMessage);
    }
}
