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

import java.util.List;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.steps.ModelingApplicationsSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.applicationNamed;
import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.applicationsNamed;

/**
 * Modeling applications scenarios
 */
public class ModelingApplications {

    @Steps
    private ModelingApplicationsSteps modelingApplicationsSteps;

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
}
