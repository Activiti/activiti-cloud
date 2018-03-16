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
import org.activiti.cloud.qa.model.modeling.Project;
import org.activiti.cloud.qa.steps.ModelingGroupsSteps;
import org.activiti.cloud.qa.steps.ModelingProjectsSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.named;

/**
 * Modeling groups scenarios
 */
public class ModelingProjects {

    @Steps
    private ModelingGroupsSteps modelingGroupsSteps;

    @Steps
    private ModelingProjectsSteps modelingProjectsSteps;

    @When("creates a project '$projectName'")
    public void createProject(String projectName) {
        Resource<Project> createdProject = modelingProjectsSteps.create(projectName);
        modelingGroupsSteps.addToCurrentContext(createdProject);
    }

    @Then("the project '$projectName' is created")
    public void checkProjectExistsInCurrentGroup(String projectName) {
        modelingProjectsSteps.checkExistsInCurrentContext(named(projectName));
    }

    @Then("delete project '$projectName'")
    public void deleteProject(String projectName) {
        modelingProjectsSteps.deleteAll(named(projectName));
    }

    @Then("delete projects '$projectNames'")
    public void deleteProjects(List<String> projectNames) {
        modelingProjectsSteps.deleteAll(named(projectNames));
    }

    @Given("an existing project '$projectName'")
    public void ensureProjectExists(String projectName) {
        if (!modelingProjectsSteps.exists(named(projectName))) {
            modelingProjectsSteps.create(projectName);
        }
    }

    @When("the user opens the project '$projectName'")
    public void openProject(String projectName) {
        modelingProjectsSteps.openModelingObject(named(projectName));
    }
}
