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

import java.util.Collection;
import java.util.List;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.model.Group;
import org.activiti.cloud.qa.steps.AuthenticationSteps;
import org.activiti.cloud.qa.steps.ModelingSteps;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resource;

/**
 * Modeling groups scenarios
 */
public class ModelingGroups {

    @Steps
    private AuthenticationSteps authenticater;

    @Steps
    private ModelingSteps modeler;

    @Given("any authenticated user")
    public void authenticate() throws Exception {
        authenticater.authenticateDefaultUser();
        authenticater.ensureUserIsAuthenticated();
    }

    @When("the group '$groupName' does't exists")
    public void deleteGroup(String groupName) {
        modeler.deleteGroup(groupName);
    }

    @Given("an existing group '$groupName'")
    public void ensureGroupExists(String groupName) {
        modeler.ensureGroupExists(groupName);
    }

    @When("the user creates a group '$groupName'")
    @Alias("creates a group '$groupName'")
    public void createGroup(String groupName) {
        modeler.createGroup(groupName);
    }

    @When("the user opens the group '$groupName'")
    public void openGroup(String groupName) {
        modeler.openGroup(groupName,
                          modeler.getAllGroups());
    }

    @Then("the group '$groupName' is created")
    public void checkGroupExists(String groupName) {
        modeler.checkGroupExists(groupName);
    }

    @Then("the subgroup '$subGroupName' is created in the current '$currentGroupName' group")
    public void checkSubGroupExistsInCurrentGroup(String subGroupName, String currentGroupName) {
        modeler.checkSubGroupExistsInCurrentGroup(subGroupName, currentGroupName);
    }

    @Then("delete groups '$groupNames'")
    public void deleteGroups(List<String> groupNames) {
        modeler.deleteGroups(groupNames);
    }
}
