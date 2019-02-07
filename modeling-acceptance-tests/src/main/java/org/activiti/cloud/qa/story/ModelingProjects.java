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

import java.io.IOException;
import java.util.List;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.qa.steps.ModelingModelsSteps;
import org.activiti.cloud.qa.steps.ModelingProjectsSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.qa.model.modeling.ModelIdentifier.identified;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.projectNamed;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.projectsNamed;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsFile;

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

    @Then("the project '$projectName' is deleted")
    public void checkProjectNotFound(String projectName) {
        modelingProjectsSteps.checkProjectNotFound(projectNamed(projectName));
    }

    @When("the user export the project")
    public void exportProject() throws IOException {
        modelingProjectsSteps.checkCurrentProjectExport();
    }

    @Given("an project '$projectName' with $modelType model '$modelName'")
    public void ensureProjectWithModelsExists(String projectName,
                                              String modelType,
                                              String modelName) {
        ensureProjectWithModelsExists(projectName,
                                      modelType,
                                      modelName,
                                      null);
    }

    @Given("an project '$projectName' with $modelType model '$modelName' version $modelVersion")
    public void ensureProjectWithModelsExists(String projectName,
                                              String modelType,
                                              String modelName,
                                              String modelVersion) {

        ensureProjectExists(projectName);
        openProject(projectName);
        if (!modelingModelsSteps.existsInCurrentContext(identified(modelName,
                                                                   modelType,
                                                                   modelVersion))) {
            resourceAsFile(modelType + "/" + setExtension(modelName,
                                                          getModelType(modelType).getContentFileExtension()))
                    .map(file -> modelingProjectsSteps.importModelInCurrentProject(file))
                    .orElseGet(() -> modelingModelsSteps.create(modelName,
                                                                modelType));
        }
    }

    @Then("the exported project contains the $modelType model $modelName")
    public void checkExportedProjectContainsModel(String modelType,
                                                  String modelName) {
        modelingProjectsSteps.checkExportedProjectContainsModel(getModelType(modelType),
                                                                modelName);
    }

    @Then("the project cannot be exported due to validation errors")
    public void checkCurrentProjectExportFailsOnValidation() throws IOException {
        modelingProjectsSteps.checkCurrentProjectExportFails("Validation errors found in project's models");
    }

    private ModelType getModelType(String modelType) {
        if (modelType.equalsIgnoreCase(PROCESS)) {
            return new ProcessModelType();
        }
        throw new RuntimeException("Unknown model type: " + modelType);
    }
}
