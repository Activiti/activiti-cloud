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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.CollectionModel;

public class ProcessInstanceVariables {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Then("variable $variableName1 has value $value1 and $variableName2 has value $value2")
    public void checkProcessInstanceVariables(
        String variableName1,
        String value1,
        String variableName2,
        String value2
    ) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                assertThat(variableName1).isNotNull();
                assertThat(variableName2).isNotNull();

                final CollectionModel<CloudVariableInstance> cloudVariableInstanceResource = getProcessVariables(
                    processInstanceId
                );

                assertThat(cloudVariableInstanceResource).isNotNull();
                assertThat(cloudVariableInstanceResource).isNotEmpty();

                assertThat(cloudVariableInstanceResource.getContent())
                    .extracting(VariableInstance::getName, VariableInstance::getValue)
                    .contains(tuple(variableName1, value1), tuple(variableName2, value2));
            });
    }

    @Then("the process variable $variableName is deleted")
    @When("the process variable $variableName is deleted")
    public void verifyProcessVariableDeleted(String variableName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                assertThat(variableName).isNotNull();
                final CollectionModel<CloudVariableInstance> variableInstances = getProcessVariables(processInstanceId);
                if (variableInstances != null) {
                    assertThat(variableInstances.getContent())
                        .extracting(VariableInstance::getName)
                        .doesNotContain(variableName);
                }
            });
    }

    @Then("the process variable $variableName is created")
    @When("the process variable $variableName is created")
    public void verifyProcessVariableCreated(String variableName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                assertThat(variableName).isNotNull();
                final CollectionModel<CloudVariableInstance> variableInstances = getProcessVariables(processInstanceId);
                assertThat(variableInstances).isNotNull();
                assertThat(variableInstances).isNotEmpty();
                //one of the variables should have name matching variableName
                assertThat(variableInstances.getContent()).extracting(VariableInstance::getName).contains(variableName);
            });
    }

    @When("the user set the instance variable $variableName1 with value $value1")
    @Then("the user set the instance variable $variableName1 with value $value1")
    public void setProcessVariables(String variableName1, String value1) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder
            .setVariables()
            .withVariable(variableName1, value1)
            .build();
        processVariablesRuntimeBundleSteps.updateVariables(processInstanceId, setProcessVariablesPayload);
    }

    public CollectionModel<CloudVariableInstance> getProcessVariables(String processInstanceId) {
        return processVariablesRuntimeBundleSteps.getVariables(processInstanceId);
    }

    @Then("query process instance variable $variableName has value $value")
    public void checkQuerykProcessInstanceVariable(String variableName, String value) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        // TODO add variable value check in processQuerySteps
        processQuerySteps.checkProcessInstanceHasVariable(processInstanceId, variableName);
    }
}
