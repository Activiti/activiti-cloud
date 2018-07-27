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

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.steps.AuthenticationSteps;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.When;

/**
 * Authentication actions
 */
public class AuthenticationActions {

    @Steps
    private AuthenticationSteps authenticationSteps;

    @Given("any authenticated user")
    @Alias("the user is authenticated as a testuser")
    public void authenticateTestUser() throws Exception {
        authenticationSteps.authenticateTestUser();
        authenticationSteps.ensureUserIsAuthenticated();
    }

    @Given("the user is authenticated as a hruser")
    public void authenticateHrUser() throws Exception {
        authenticationSteps.authenticateHrUser();
        authenticationSteps.ensureUserIsAuthenticated();
    }

    @Given("the user is authenticated as a hradmin")
    public void authenticateHrAdmin() throws Exception {
        authenticationSteps.authenticateHrAdmin();
        authenticationSteps.ensureUserIsAuthenticated();
    }


}
