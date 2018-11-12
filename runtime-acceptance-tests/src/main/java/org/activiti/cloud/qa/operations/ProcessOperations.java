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

    //TODO: change the reference to this method once the previous one is deleted
    @When("A - the user starts a $processName")
    public void startProcess(String processName) {

        ProcessInstance processInstance = processRuntimeBundleSteps.startProcess(
                processDefinitionKeyMatcher(processName));

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("B - the user starts a $processName with name $instanceName and businessKey $businessKey")
    public void startProcessWithNameAndBusinessKey(String processName, String instanceName, String businessKey){
        ProcessInstance processInstance = processRuntimeBundleSteps.startProcessWithNameAndBusinessKey(
                processDefinitionKeyMatcher(processName), instanceName, businessKey);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("C - the user starts a $processName with variables")
    public void startProcessWithVariables(String processName){
        ProcessInstance processInstance = processRuntimeBundleSteps.startProcessWithVariables(
                processDefinitionKeyMatcher(processName));

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }
}
