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

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.story.step.IdentityManagementSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Identity Management projects scenarios
 */
public class IdentityManagementActions {

    @Steps
    private IdentityManagementSteps identityManagementSteps;

    @When("the user retrieves his roles")
    public void getRoles() {
        identityManagementSteps.getRoles();
    }

    @Then("roles list contains global role $role")
    public void containsGlobalRole(String role) {
        identityManagementSteps.containsGlobalAccessRole(role);
    }

    @When("the user searches for users containing $searchKey")
    public void searchUsers(String searchKey) {
        identityManagementSteps.searchUsers(searchKey);
    }

    @Then("user search contains $username")
    public void usersSearchContains(String username) {
        identityManagementSteps.usersSearchResultContains(username);
    }

    @Then("user search does not contain $username")
    public void usersSearchDoesNotContain(String username) {
        identityManagementSteps.usersSearchResultDoesNotContain(username);
    }

    @When("the user searches for groups containing $searchKey")
    public void searchGroups(String searchKey) {
        identityManagementSteps.searchGroups(searchKey);
    }

    @Then("group search contains $groupName")
    public void groupsSearchContains(String groupName) {
        identityManagementSteps.groupsSearchResultContains(groupName);
    }

    @Then("group search does not contain $groupName")
    public void groupsSearchDoesNotContain(String groupName) {
        identityManagementSteps.groupsSearchResultDoesNotContain(groupName);
    }
}
