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
import java.util.Optional;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.shared.rest.error.ExpectRestError;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.qa.steps.ModelingApplicationsSteps;
import org.activiti.cloud.qa.steps.ModelingModelsSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.organization.api.ProcessModelType.BPMN20_XML;
import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.qa.model.modeling.ModelIdentifier.identified;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.applicationNamed;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.applicationsNamed;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsFile;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

/**
 * Modeling applications scenarios
 */
public class ModelingApplications {

    @Steps
    private ModelingApplicationsSteps modelingApplicationsSteps;

    @Steps
    private ModelingModelsSteps modelingModelsSteps;

    @When("the user creates an application '$applicationName'")
    public void createApplication(String applicationName) {
        modelingApplicationsSteps.create(applicationName);
    }

    @When("update the application name to '$applicationName'")
    public void updateApplicationName(String applicationName) {
        modelingApplicationsSteps.updateApplicationName(applicationName);
    }

    @Then("the application name is updated to '$applicationName'")
    public void checkApplicationName(String applicationName) {
        modelingApplicationsSteps.checkCurrentApplicationName(applicationName);
    }

    @Then("the application '$applicationName' is created")
    public void checkApplicationExists(String applicationName) {
        modelingApplicationsSteps.checkExists(applicationNamed(applicationName));
    }

    @Then("delete application '$applicationName'")
    @When("the user delete the application '$applicationName'")
    public void deleteApplication(String applicationName) {
        modelingApplicationsSteps.deleteAll(applicationNamed(applicationName));
    }

    @Then("delete applications '$applicationNames'")
    public void deleteApplications(List<String> applicationNames) {
        modelingApplicationsSteps.deleteAll(applicationsNamed(applicationNames));
    }

    @Given("an existing application '$applicationName'")
    public void ensureApplicationExists(String applicationName) {
        if (!modelingApplicationsSteps.exists(applicationNamed(applicationName))) {
            modelingApplicationsSteps.create(applicationName);
        }
    }

    @When("the user opens the application '$applicationName'")
    public void openApplication(String applicationName) {
        modelingApplicationsSteps.openModelingObject(applicationNamed(applicationName));
    }

    @Then("the application '$applicationName' is deleted")
    public void checkApplicationNotFound(String applicationName) {
        modelingApplicationsSteps.checkApplicationNotFound(applicationNamed(applicationName));
    }

    @When("the user export the application")
    public void exportApplication() throws IOException {
        modelingApplicationsSteps.exportCurrentApplication();
    }

    @Given("an application '$applicationName' with $modelType model '$modelName'")
    public void ensureApplicationWithModelsExists(String applicationName,
                                                  String modelType,
                                                  String modelName) {
        ensureApplicationWithModelsExists(applicationName,
                                          modelType,
                                          modelName,
                                          null);
    }

    @Given("an application '$applicationName' with $modelType model '$modelName' version $modelVersion")
    public void ensureApplicationWithModelsExists(String applicationName,
                                                  String modelType,
                                                  String modelName,
                                                  String modelVersion) {

        ensureApplicationExists(applicationName);
        openApplication(applicationName);
        if (!modelingModelsSteps.existsInCurrentContext(identified(modelName,
                                                                   modelType,
                                                                   modelVersion))) {
            resourceAsFile(modelType + "/" + setExtension(modelName,
                                                          getModelType(modelType).getContentFileExtension()))
                    .map(file -> modelingApplicationsSteps.importModelInCurrentApplication(file))
                    .orElseGet(() -> modelingModelsSteps.create(modelName,
                                                                modelType));
        }
    }

    @Then("the exported application contains the $modelType model $modelName")
    public void checkExportedApplicationContainsModel(String modelType,
                                                      String modelName) {
        modelingApplicationsSteps.checkExportedApplicationContainsModel(getModelType(modelType),
                                                                        modelName);
    }

    @Then("the application cannot be exported due to validation errors")
    @ExpectRestError(statusCode = SC_BAD_REQUEST, value = "Validation errors found in application's models")
    public void checkCurrentApplicationExportFailsOnValidation() throws IOException {
        modelingApplicationsSteps.exportCurrentApplication();
    }

    private ModelType getModelType(String modelType) {
        if(modelType.equalsIgnoreCase(PROCESS)) {
            return new ProcessModelType();
        }
        throw new RuntimeException("Unknown model type: " + modelType);
    }
}
