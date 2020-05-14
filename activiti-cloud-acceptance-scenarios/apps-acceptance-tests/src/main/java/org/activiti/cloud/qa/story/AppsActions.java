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
import org.activiti.cloud.qa.steps.AppsServiceSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class AppsActions {

    @Steps
    private AppsServiceSteps appsServiceSteps;

    @Given("the app service is up")
    public void appServiceIsRunning(){
        appsServiceSteps.checkAppsServiceHealth();
    }

    @Then("the status of the app is shown as running")
    public void appIsShownAsRunning(){
        appsServiceSteps.checkAppsServiceShowsApp("default-app");
    }
}
