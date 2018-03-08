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
import java.util.Map;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.model.EventType;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.QueryStatus;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.*;

public class ProcessInstanceTasks {

    @Steps
    private RuntimeBundleSteps runtimeBundleSteps;

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private QuerySteps querySteps;

    private ProcessInstance processInstance;
    private Task currentTask;

    @When("services are started")
    public void checkServicesStatus() {
        assertThat(isServiceUp(runtimeBundleSteps.health())).isTrue();
        assertThat(isServiceUp(auditSteps.health())).isTrue();
        assertThat(isServiceUp(querySteps.health())).isTrue();
    }

    @When("the user starts a random process")
    public void startProcess() throws Exception {
        processInstance = runtimeBundleSteps.startProcess();
        assertThat(processInstance).isNotNull();

        List<Task> tasks = new ArrayList<>(
                runtimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId()));

        assertThat(tasks).isNotEmpty();
        currentTask = tasks.get(0);
        assertThat(currentTask).isNotNull();
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
                                              QueryStatus.COMPLETED);
        auditSteps.checkProcessInstanceTaskEvent(processInstance.getId(),
                                                 currentTask.getId(),
                                                 EventType.TASK_COMPLETED);
    }

    @When("cancel the process")
    public void cancelProcessInstance() throws Exception {
        runtimeBundleSteps.deleteProcessInstance(processInstance.getId());
    }

    @Then("the process instance is cancelled")
    public void verifyProcessInstanceIsDeleted() throws Exception {
        runtimeBundleSteps.checkProcessInstanceNotFound(processInstance.getId());
        runtimeBundleSteps.waitForMessagesToBeConsumed();
        querySteps.checkProcessInstanceStatus(processInstance.getId(),
                                              QueryStatus.CANCELLED);
        auditSteps.checkProcessInstanceEvent(processInstance.getId(),
                                             EventType.PROCESS_CANCELLED);
    }

    private boolean isServiceUp(Map<String, Object> appInfo) {
        if (appInfo != null) {
            String status = appInfo.get("status").toString();
            if (!"".equals(status) && "UP".equals(status)) {
                return true;
            }
        }
        return false;
    }

}
