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

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessVariablesRuntimeAdminSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.http.ResponseEntity;

public class ProcessInstanceVariablesAdmin {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessVariablesRuntimeAdminSteps processVariablesRuntimeAdminSteps;

    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;

    @When("the admin starts the process $processName with variables $variableName1 and $variableName2")
    public void adminStartProcess(String processName, String variableName1, String variableName2) {
        Map<String, Object> variables = getVariablesMap(variableName1, variableName1, variableName2, variableName2);
        ProcessInstance processInstance = processRuntimeBundleSteps.startProcessWithVariables(
            processDefinitionKeyMatcher(processName),
            variables
        );

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When(
        "the admin update the instance variables $variableName1 with value $value1 and $variableName2 with value $value2"
    )
    public void adminUpdateVariables(String variableName1, String value1, String variableName2, String value2) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        Map<String, Object> variables = getVariablesMap(variableName1, value1, variableName2, value2);

        SetProcessVariablesPayload setProcessVariablesPayload = new SetProcessVariablesPayload();
        setProcessVariablesPayload.setProcessInstanceId(processInstanceId);
        setProcessVariablesPayload.setVariables(variables);

        ResponseEntity<List<String>> updateVarsErrorMessages = processVariablesRuntimeAdminSteps.updateVariables(
            processInstanceId,
            setProcessVariablesPayload
        );

        Serenity.setSessionVariable("updateVarsErrorMessages").to(updateVarsErrorMessages.getBody());
    }

    @When("the admin delete the instance variable $variableName")
    public void adminDeleteVariables(String variableName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        RemoveProcessVariablesPayload removeProcessVariablesPayload = ProcessPayloadBuilder
            .removeVariables()
            .withVariableNames(variableName)
            .build();

        processVariablesRuntimeAdminSteps.removeVariables(processInstanceId, removeProcessVariablesPayload);
    }

    @Then("the list of errors messages is empty")
    public void checkUpdateVarsErrorMessagesIsEmpty() {
        List<String> updateVarsErrorMessages = Serenity.sessionVariableCalled("updateVarsErrorMessages");

        if (updateVarsErrorMessages != null) {
            assertThat(updateVarsErrorMessages).isEmpty();
        }
    }

    private Map<String, Object> getVariablesMap(
        String variableName1,
        String variableValue1,
        String variableName2,
        String variableValue2
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName1, variableValue1);
        variables.put(variableName2, variableValue2);
        return variables;
    }
}
