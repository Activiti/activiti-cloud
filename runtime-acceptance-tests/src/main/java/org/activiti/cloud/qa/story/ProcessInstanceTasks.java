/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.story;

import java.util.ArrayList;
import java.util.List;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.model.EventType;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.ProcessInstanceStatus;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.rest.error.ExpectRestError;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.DEFAULT_PROCESS_INSTANCE_KEY;
import static org.assertj.core.api.Assertions.*;

public class ProcessInstanceTasks {

    @Steps
    private RuntimeBundleSteps runtimeBundleSteps;

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private QuerySteps querySteps;

    private ProcessInstance processInstance;

    private String processInstanceDiagram;

    private Task currentTask;

    @When("services are started")
    public void checkServicesStatus() {
        runtimeBundleSteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
        querySteps.checkServicesHealth();
    }

    @When("the user starts process '$process'")
    public void startProcess(String process) throws Exception {
        processInstance = runtimeBundleSteps.startProcess(process);
        assertThat(processInstance).isNotNull();

        List<Task> tasks = new ArrayList<>(
                runtimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId()));

        assertThat(tasks).isNotEmpty();
        currentTask = tasks.get(0);
        assertThat(currentTask).isNotNull();
    }

    @When("the user starts a random process")
    public void startProcess() throws Exception {
        this.startProcess(DEFAULT_PROCESS_INSTANCE_KEY);
    }

    @When("the user starts a process without graphic info")
    public void startProcessWithoutFGraphicInfo() throws Exception {
        this.startProcess("fixSystemFailure");
    }

    @When("the user claims a task")
    public void claimTask() throws Exception {
        runtimeBundleSteps.assignTaskToUser(currentTask.getId(),
                                            "hruser");
    }

    @When("the user completes the task")
    public void completeTask() throws Exception {
        runtimeBundleSteps.completeTask(currentTask.getId());
    }

    @Then("the status of the process is changed to completed")
    public void verifyProcessStatus() throws Exception {
        runtimeBundleSteps.waitForMessagesToBeConsumed();
        querySteps.checkProcessInstanceStatus(processInstance.getId(),
                                              ProcessInstanceStatus.COMPLETED);
        auditSteps.checkProcessInstanceTaskEvent(processInstance.getId(),
                                                 currentTask.getId(),
                                                 EventType.TASK_COMPLETED);
    }

    @When("the user cancel the process")
    @Alias("cancel the process")
    public void cancelCurrentProcessInstance() throws Exception {
        runtimeBundleSteps.deleteProcessInstance(processInstance.getId());
    }

    @Then("the process instance is cancelled")
    public void verifyProcessInstanceIsDeleted() throws Exception {
        runtimeBundleSteps.checkProcessInstanceNotFound(processInstance.getId());
        runtimeBundleSteps.waitForMessagesToBeConsumed();
        querySteps.checkProcessInstanceStatus(processInstance.getId(),
                                              ProcessInstanceStatus.CANCELLED);
        auditSteps.checkProcessInstanceEvent(processInstance.getId(),
                                             EventType.PROCESS_CANCELLED);
    }

    @When("open the process diagram")
    public void openProcessInstanceDiagram() {
        processInstanceDiagram = runtimeBundleSteps.openProcessInstanceDiagram(processInstance.getId());
    }

    @Then("the diagram is shown")
    public void checkProcessInstanceDiagram() throws Exception {
        runtimeBundleSteps.checkProcessInstanceDiagram(processInstanceDiagram);
    }

    @Then("no diagram is shown")
    public void checkProcessInstanceNoDiagram() throws Exception {
        runtimeBundleSteps.checkProcessInstanceNoDiagram(processInstanceDiagram);
    }
    @Given("any suspended process instance")
    public void suspendCurrentProcessInstance() {
        processInstance = runtimeBundleSteps.startProcess();
        runtimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

    @When("activate the process")
    public void activateCurrentProcessInstance() {
        runtimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

    @Then("the process cannot be activated anymore")
    @ExpectRestError("Unable to find process instance for the given id")
    public void cannotActivateProcessInstance() {
        runtimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

}
