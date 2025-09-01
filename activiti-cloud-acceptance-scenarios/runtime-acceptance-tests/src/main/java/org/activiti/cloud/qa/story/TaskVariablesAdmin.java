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

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.core.steps.runtime.admin.TaskVariablesRuntimeAdminSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.CollectionModel;

public class TaskVariablesAdmin {

    @Steps
    private TaskVariablesRuntimeAdminSteps taskVariablesRuntimeAdminSteps;

    @Given("the user creates, using admin endpoint, a task variable named $variableName with value $variableValue")
    @When("the user creates, using admin endpoint, a task variable named $variableName with value $variableValue")
    public void createTaskVariable(String variableName, String variableValue) {
        taskVariablesRuntimeAdminSteps.createVariable(
            Serenity.sessionVariableCalled(Tasks.STAND_ALONE_TASK_ID),
            variableName,
            variableValue
        );
    }

    @When("the user updates, using admin endpoint, the task variable named $variableName with value $variableValue")
    public void updateTaskVariable(String variableName, String variableValue) {
        taskVariablesRuntimeAdminSteps.updateVariable(
            Serenity.sessionVariableCalled(Tasks.STAND_ALONE_TASK_ID),
            variableName,
            variableValue
        );
    }

    @Then(
        "the user is able to retrieve, using the admin endpoint, a variable named $variableName with value $variableValue as part of task variables"
    )
    public void assertHasTaskVariable(String variableName, String variableValue) {
        String taskId = Serenity.sessionVariableCalled(Tasks.STAND_ALONE_TASK_ID);
        CollectionModel<CloudVariableInstance> rbVariables = taskVariablesRuntimeAdminSteps.getVariables(taskId);
        assertThat(rbVariables)
            .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
            .contains(tuple(variableName, variableValue));
    }
}
