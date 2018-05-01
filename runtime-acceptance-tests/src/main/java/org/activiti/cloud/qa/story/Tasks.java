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

import java.util.Collection;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.model.TaskStatus;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.*;

public class Tasks {

    @Steps
    private RuntimeBundleSteps runtimeBundleSteps;

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private QuerySteps querySteps;

    /**
     * standalone task
     */
    private Task newTask;

    /**
     * subtask of {@link #newTask}
     */
    private Task subtask;

    @When("the user creates a standalone task")
    @Given("an existing standalone task")
    public void createTask() throws Exception {
        newTask = runtimeBundleSteps.createNewTask();
        assertThat(newTask).isNotNull();
    }

    @Then("the task is created and the status is assigned")
    public void taskIsCreatedAndAssigned() {
        final Task assignedTask = runtimeBundleSteps.getTaskById(newTask.getId());
        assertThat(assignedTask).isNotNull();
        assertThat(assignedTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        auditSteps.checkTaskCreatedAndAssignedEvents(assignedTask.getId());
        querySteps.checkTaskStatus(assignedTask.getId(),
                                   TaskStatus.ASSIGNED);
    }

    @When("the user cancel the task")
    public void cancelCurrentTask() {
        runtimeBundleSteps.deleteTask(newTask.getId());
    }

    @Then("the task is cancelled")
    public void checkTaskIsCancelled() throws Exception {
        runtimeBundleSteps.checkTaskNotFound(newTask.getId());
        runtimeBundleSteps.waitForMessagesToBeConsumed();
        auditSteps.checkTaskDeletedEvent(newTask.getId());
        querySteps.checkTaskStatus(newTask.getId(),
                                   TaskStatus.CANCELLED);
    }

    @When("user creates a subtask for the previously created task")
    public void createASubtask() throws Exception {
        subtask = runtimeBundleSteps.createSubtask(newTask.getId());
        assertThat(subtask).isNotNull();
    }

    @Then("the subtask is created and references another task")
    public void taskWithSubtaskIsCreated() {
        final Task createdSubtask = runtimeBundleSteps.getTaskById(subtask.getId());
        assertThat(createdSubtask).isNotNull();
        assertThat(createdSubtask.getParentTaskId()).isNotEmpty().isEqualToIgnoringCase(newTask.getId());

        auditSteps.checkSubtaskCreated(createdSubtask.getId(),
                                       newTask.getId());
        querySteps.checkSubtaskHasParentTaskId(subtask.getId(),
                                               newTask.getId());
    }

    @Then("a list of one subtask should be available for the task")
    public void getSubtasksForTask() {
        final Collection subtasks = runtimeBundleSteps.getSubtasks(newTask.getId()).getContent();
        assertThat(subtasks).isNotNull().hasSize(1);
        assertThat(subtasks.iterator().hasNext()).isTrue();
        assertThat(subtasks.iterator().next()).extracting("id").containsOnly(subtask.getId());
    }
}
