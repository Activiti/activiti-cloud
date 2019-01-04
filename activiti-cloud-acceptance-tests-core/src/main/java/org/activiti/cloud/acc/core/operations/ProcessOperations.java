package org.activiti.cloud.acc.core.operations;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.operations.steps.runtime.ProcessRuntimeSteps;
import org.jbehave.core.annotations.When;
import java.io.IOException;


public class ProcessOperations {

    @Steps
    private ProcessRuntimeSteps processRuntimeSteps;

    @When("the user starts a process called $processDefinitionName")
    public void startProcessInstance(String processDefinitionName) throws IOException{
        ProcessInstance processInstance = processRuntimeSteps.startProcess(processDefinitionName);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("the user deletes the process instance")
    public void deleteProcessInstance(){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        processRuntimeSteps.deleteProcessInstance(processInstanceId);
    }

    @When("the user suspends the process instance")
    public void suspendProcessInstance(){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        processRuntimeSteps.suspendProcessInstance(processInstanceId);
    }

    @When("the user sets variables")
    public void setVariables(){
        Serenity.setSessionVariable("variables").to(true);
    }
}
