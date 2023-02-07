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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.api.task.model.CloudTask;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProcessInstanceInclusiveGateway {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private TaskQuerySteps taskQuerySteps;

    @Steps
    private AuditSteps auditSteps;

    private ProcessInstance processInstance;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When(
        "the user starts a process with inclusive gateway $processName and set $variableName variable value to $variableValue"
    )
    public void startProcess(String processName, String variableName, Integer variableValue)
        throws IOException, InterruptedException {
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, variableValue);
        processInstance =
            processRuntimeBundleSteps.startProcessWithVariables(processDefinitionKeyMatcher(processName), variables);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("the task is created $taskName")
    public void checkProcessTaskIsCreated(String taskName) throws Exception {
        Task task = getTaskByName(taskName);
        assertThat(task).isNotNull();
    }

    public void claimTaskById(String taskId) throws Exception {
        taskRuntimeBundleSteps.claimTask(taskId);
        checkTaskStatus(taskId, Task.TaskStatus.ASSIGNED);
    }

    public void completeTaskById(String taskId) throws Exception {
        taskRuntimeBundleSteps.completeTask(taskId, TaskPayloadBuilder.complete().withTaskId(taskId).build());
        checkTaskStatus(taskId, Task.TaskStatus.COMPLETED);
    }

    @When("the user claims and completes the task $taskName")
    public void claimCompleteTask(String taskName) throws Exception {
        Task task = getTaskByName(taskName);
        assertThat(task).isNotNull();

        claimTaskById(task.getId());
        completeTaskById(task.getId());
    }

    @Then("events are emitted for the inclusive gateway $gatewayId")
    public void verifyInclusiveGatewayEventsEmiited(String gatewayId) throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        auditSteps.checkProcessInstanceInclusiveGatewayEvents(processId, gatewayId);
    }

    @Then("the user will see $number tasks")
    public void checkNumberOfTasks(Integer number) throws Exception {
        List<Task> tasks = getTasks();
        assertThat(tasks.size()).isEqualTo(number);
    }

    public List<Task> getTasks() throws Exception {
        assertThat(processInstance).isNotNull();

        return new ArrayList<>(processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId()));
    }

    public Task getTaskByName(String taskName) throws Exception {
        List<Task> tasks = getTasks();
        assertThat(tasks).isNotEmpty();

        return tasks.stream().filter(t -> t.getName().equals(taskName)).findFirst().orElse(null);
    }

    public void checkTaskStatus(String taskId, Task.TaskStatus status) throws Exception {
        if (status != TaskStatus.COMPLETED) {
            final CloudTask task = taskRuntimeBundleSteps.getTaskById(taskId);
            assertThat(task).isNotNull();
            assertThat(task.getStatus()).isEqualTo(status);
        }
        taskQuerySteps.checkTaskStatus(taskId, status);
    }

    @Then("the process with inclusive gateway is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }
}
