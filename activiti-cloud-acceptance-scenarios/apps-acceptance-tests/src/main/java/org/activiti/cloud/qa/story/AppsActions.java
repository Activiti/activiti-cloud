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
