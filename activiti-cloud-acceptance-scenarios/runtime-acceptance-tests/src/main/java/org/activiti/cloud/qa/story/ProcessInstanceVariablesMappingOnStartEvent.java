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
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskVariableRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.CollectionModel;

public class ProcessInstanceVariablesMappingOnStartEvent {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;

    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;

    @Steps
    private TaskVariableRuntimeBundleSteps taskVariableRuntimeBundleSteps;

    public ProcessInstanceVariablesMappingOnStartEvent() throws ParseException {}

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processVariablesRuntimeBundleSteps.checkServicesHealth();
        taskRuntimeBundleSteps.checkServicesHealth();
    }

    @When("the user starts variables mapping process on start event")
    public void startProcessWithVariablesMappingOnStartEvent() throws ParseException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("Text0xfems", "Form name");
        variables.put("Text0rvs0o", "Form email");

        ProcessInstance processInstance = processRuntimeBundleSteps.startProcessWithVariables(
            processDefinitionKeyMatcher("PROCESS_START_EVENT_VARIABLE_MAPPING"),
            variables
        );

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("process variables are properly mapped on start event")
    public void checkProcessInstanceVariablesMapping() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                final CollectionModel<CloudVariableInstance> variables = processVariablesRuntimeBundleSteps.getVariables(
                    processInstanceId
                );

                assertThat(variables)
                    .isNotNull()
                    .hasSize(2)
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .containsOnly(tuple("name", "Form name"), tuple("email", "Form email"));
            });
    }

    @Then("process variables are properly mapped to the task variables")
    public void checkTaskVariablesMapping() throws Exception {
        List<Task> tasks = getTasks();

        assertThat(tasks).isNotNull().hasSize(1);

        Serenity.setSessionVariable("taskId").to(tasks.get(0).getId());

        await()
            .untilAsserted(() -> {
                final Collection<CloudVariableInstance> variables = taskVariableRuntimeBundleSteps.getVariables(
                    tasks.get(0).getId()
                );

                assertThat(variables)
                    .isNotNull()
                    .hasSize(2)
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .containsOnly(tuple("Text0xfems", "Form name"), tuple("Text0rvs0o", "Form email"));
            });
    }

    @Then("the user may complete the task")
    public void completeTask() throws Exception {
        String taskId = Serenity.sessionVariableCalled("taskId");
        taskRuntimeBundleSteps.completeTask(taskId, TaskPayloadBuilder.complete().withTaskId(taskId).build());
    }

    public List<Task> getTasks() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        return new ArrayList<>(processRuntimeBundleSteps.getTaskByProcessInstanceId(processId));
    }
}
