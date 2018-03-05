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
import org.activiti.cloud.qa.model.Event;
import org.activiti.cloud.qa.model.EventType;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.qa.model.QueryStatus.COMPLETED;

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
        List<Event> events = new ArrayList<>(
                auditSteps.getEventsByProcessInstanceIdAndEventType(processInstance.getId(),
                                                                    EventType.TASK_COMPLETED));

        assertThat(events).isNotEmpty();
        Event resultingEvent = events.get(0);
        assertThat(resultingEvent).isNotNull();
        assertThat(resultingEvent.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(resultingEvent.getEventType()).isEqualTo(EventType.TASK_COMPLETED);
        assertThat(resultingEvent.getTask().getId()).isEqualTo(currentTask.getId());
    }

    @Then("the status of the process is changed to completed by querying")
    public void verifyProcessStatusByQuery() throws Exception {
        ProcessInstance instance = querySteps.getProcessInstance(processInstance.getId());

        assertThat(instance).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(COMPLETED);
    }

}
