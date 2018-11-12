package org.activiti.cloud.qa.operations;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.jbehave.core.annotations.When;
import steps.runtime.ProcessRuntimeBundleSteps;

import static helper.ProcessDefinitionRegistry.processDefinitionKeyMatcher;

public class ProcessOperations {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @When("A - the user starts a $processName")
    public void startProcess(String processName) {

        ProcessInstance processInstance = processRuntimeBundleSteps.startProcess(
                processDefinitionKeyMatcher(processName));

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());

        System.out.println("process has been created with id: " + processInstance.getId());
    }
}
