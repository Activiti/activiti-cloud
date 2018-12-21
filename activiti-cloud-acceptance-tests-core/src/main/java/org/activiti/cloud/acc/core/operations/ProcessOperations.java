package org.activiti.cloud.acc.core.operations;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.jbehave.core.annotations.When;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import java.io.IOException;

public class ProcessOperations {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    //TODO: change the reference to this method once the previous one is deleted
    @When("the user starts a process called $processName")
    public void startProcess(String processName) throws IOException {

        ProcessInstance processInstance = processRuntimeBundleSteps.startProcess(
                processName, false);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("the user starts a process with variables called $processName")
    public void startProcessWithVariables(String processName) throws IOException {
        ProcessInstance processInstance = processRuntimeBundleSteps.startProcess(
                processName, true);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }
}
