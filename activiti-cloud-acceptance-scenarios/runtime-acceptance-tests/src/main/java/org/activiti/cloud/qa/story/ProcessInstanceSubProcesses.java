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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessVariablesRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.CollectionModel;

public class ProcessInstanceSubProcesses {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessVariablesRuntimeBundleSteps processVariablesRuntimeBundleSteps;

    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private AuditSteps auditSteps;

    private ProcessInstance processInstance;

    private ProcessInstance subprocessInstance;

    private Task currentTask;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts a process with tasks and a subProcess called $processName")
    public void startProcessWithTaks(String processName) throws IOException, InterruptedException {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName), false);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
        checkProcessWithTaskCreated();
    }

    @When("the user starts a process with a subProcess called $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName), false);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    private void checkProcessWithTaskCreated() {
        assertThat(processInstance).isNotNull();

        List<Task> tasks = new ArrayList<>(
            processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId())
        );
        assertThat(tasks).isNotEmpty();
        currentTask = tasks.get(0);
        assertThat(currentTask).isNotNull();

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("the user claims the task declared in the subprocess")
    public void claimTask() throws Exception {
        taskRuntimeBundleSteps.claimTask(currentTask.getId());
    }

    @When("the user completes the task declared in the subprocess")
    public void completeTask() throws Exception {
        taskRuntimeBundleSteps.completeTask(
            currentTask.getId(),
            TaskPayloadBuilder.complete().withTaskId(currentTask.getId()).build()
        );
    }

    @Then("subProcess events are emitted")
    public void verifySubProcessEventsEmiited() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        auditSteps.checkProcessInstanceSubProcessEvents(processId);
    }

    @Then("the process with embedded subprocess is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }

    @Then("the subprocess has been created")
    public void checkSubProcessHasBeenCreated() {
        String parentProcessId = Serenity.sessionVariableCalled("processInstanceId");

        subprocessInstance = processRuntimeBundleSteps.getSubProcesses(parentProcessId).iterator().next();
        assertThat(subprocessInstance).isNotNull();
        assertThat(subprocessInstance.getParentId()).isEqualTo(parentProcessId);

        Serenity.setSessionVariable("subProcessInstanceId").to(subprocessInstance.getId());
    }

    @Then("a subprocess variable $variableName is created with value $variableValue")
    public void checkSubProcessInstanceVariable(String variableName, String variableValue) {
        assertThat(subprocessInstance).isNotNull();
        await()
            .untilAsserted(() -> {
                CollectionModel<CloudVariableInstance> processVariables = processVariablesRuntimeBundleSteps.getVariables(
                    subprocessInstance.getId()
                );
                assertThat(processVariables.getContent()).isNotNull();
                assertThat(processVariables.getContent())
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .contains(tuple(variableName, variableValue));
            });
    }

    @Then("the parent process instance has a variable named $variableName with value $variableValue")
    public void checkParentProcessInstanceVariable(String variableName, String variableValue) {
        await()
            .untilAsserted(() -> {
                CollectionModel<CloudVariableInstance> processVariables = processVariablesRuntimeBundleSteps.getVariables(
                    processInstance.getId()
                );
                assertThat(processVariables.getContent()).isNotNull();
                assertThat(processVariables.getContent())
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .contains(tuple(variableName, variableValue));
            });
    }

    @When("the user claims and completes the subprocess task $taskName")
    public void claimAndCompleteSubprocessTask(String taskName) throws Exception {
        currentTask =
            processRuntimeBundleSteps.getTaskByProcessInstanceId(subprocessInstance.getId()).iterator().next();
        assertThat(currentTask.getName()).isEqualTo(taskName);
        claimTask();
        completeTask();
    }
}
