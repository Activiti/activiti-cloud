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
package org.activiti.cloud.acc.core.operations;

import java.io.IOException;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.operations.steps.runtime.ProcessRuntimeSteps;
import org.jbehave.core.annotations.When;

public class ProcessOperations {

    @Steps
    private ProcessRuntimeSteps processRuntimeSteps;

    @When("the user starts a process called $processDefinitionName")
    public void startProcessInstance(String processDefinitionName) throws IOException {
        ProcessInstance processInstance = processRuntimeSteps.startProcess(processDefinitionName);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("the user deletes the process instance")
    public void deleteProcessInstance() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        processRuntimeSteps.deleteProcessInstance(processInstanceId);
    }

    @When("the user suspends the process instance")
    public void suspendProcessInstance() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        processRuntimeSteps.suspendProcessInstance(processInstanceId);
    }

    @When("the user sets variables")
    public void setVariables() {
        Serenity.setSessionVariable("variables").to(true);
    }
}
