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
import org.activiti.cloud.qa.model.modeling.Group;
import org.activiti.cloud.qa.steps.ModelingGroupsSteps;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.qa.model.modeling.ModelingNamingIdentifier.named;

/**
 * Modeling groups scenarios
 */
public class ModelingGroups {

    @Steps
    private ModelingGroupsSteps modelingGroupsSteps;

    @When("the user creates a group '$groupName'")
    @Alias("creates a group '$groupName'")
    public void createGroup(String groupName) {
        Resource<Group> createdGroup = modelingGroupsSteps.create(groupName);
        modelingGroupsSteps.addToCurrentContext(createdGroup);
    }

    @Given("an existing group '$groupName'")
    public void ensureGroupExists(String groupName) {
        if (!modelingGroupsSteps.exists(named(groupName))) {
            modelingGroupsSteps.create(groupName);
        }
    }

    @When("the user opens the group '$groupName'")
    public void openGroup(String groupName) {
        modelingGroupsSteps.openModelingObject(named(groupName));
    }

    @Then("the group '$groupName' is created")
    public void checkGroupExists(String groupName) {
        modelingGroupsSteps.checkExists(named(groupName));
    }

    @Then("the subgroup '$subGroupName' is created")
    public void checkSubGroupExistsInCurrentGroup(String subGroupName) {
        modelingGroupsSteps.checkExistsInCurrentContext(named(subGroupName));
    }

    @When("the group '$groupName' does't exists")
    @Then("delete group '$groupName'")
    public void deleteGroup(String groupName) {
        modelingGroupsSteps.deleteAll(named(groupName));
    }

    @Then("delete groups '$groupNames'")
    public void deleteGroups(List<String> groupNames) {
        modelingGroupsSteps.deleteAll(named(groupNames));
    }
}
