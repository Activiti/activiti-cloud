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

package org.activiti.cloud.qa.modeling;

import java.io.IOException;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.modeling.model.Group;
import org.activiti.cloud.qa.user.UserSessionSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * User modeling actions
 */
public class ModelingActions {

    @Steps
    private UserSessionSteps userSessionSteps;

    @Steps
    private ModelingSteps modeler;

    @Given("any authenticated user")
    public void authenticate() throws IOException {
        userSessionSteps.authenticateDefaultUser();
        userSessionSteps.ensureUserIsAuthenticated();
    }

    @When("the user creates a group")
    public void createTopLevelGroup() {
        modeler.createGroup("group1",
                            "Group1");
    }

    @Then("the group is created")
    public void checkGroupExists() {
        Group group = modeler.findGroupById("group1");
        modeler.checkGroupIsExpectedOne(group, "Group1");
    }
}
