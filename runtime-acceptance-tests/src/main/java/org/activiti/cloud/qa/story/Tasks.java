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
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.assertThat;

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
    private CloudTask newTask;

    /**
     * subtask of {@link #newTask}
     */
    private Task subtask;

    @When("the user creates a standalone task")
    @Given("the user creates a standalone task")
    public void createTask() throws Exception {
        newTask = runtimeBundleSteps.createNewTask();
        assertThat(newTask).isNotNull();
    }

    @Then("the task is created and the status is assigned")
    public void taskIsCreatedAndAssigned() throws Exception {
        final CloudTask assignedTask = runtimeBundleSteps.getTaskById(newTask.getId());
        assertThat(assignedTask).isNotNull();
        assertThat(assignedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
        auditSteps.checkTaskCreatedAndAssignedEventsWhenAlreadyAssinged(assignedTask.getId());
        querySteps.checkTaskStatus(assignedTask.getId(),
                                   Task.TaskStatus.ASSIGNED);
    }

    @When("the user deletes the standalone task")
    public void deleteCurrentTask() {
        runtimeBundleSteps.deleteTask(newTask.getId());
    }

    @Then("the standalone task is deleted")
    public void checkTaskIsDeleted() throws Exception {
        runtimeBundleSteps.checkTaskNotFound(newTask.getId());
        auditSteps.checkTaskDeletedEvent(newTask.getId());
        querySteps.checkTaskStatus(newTask.getId(),
                                   //TODO change to DELETED when RB is ready
                                   Task.TaskStatus.CANCELLED);
    }

    @When("user creates a subtask for the previously created task")
    public void createASubtask() throws Exception {
        subtask = runtimeBundleSteps.createSubtask(newTask.getId());
        assertThat(subtask).isNotNull();
        assertThat(subtask.getParentTaskId()).isNotNull();
        assertThat(subtask.getParentTaskId()).isNotEmpty();
    }

    @Then("the subtask is created and references another task")
    public void taskWithSubtaskIsCreated()  throws Exception {
        final CloudTask createdSubtask = runtimeBundleSteps.getTaskById(subtask.getId());
        assertThat(createdSubtask).isNotNull();
        assertThat(createdSubtask.getParentTaskId()).isNotEmpty().isEqualToIgnoringCase(newTask.getId());
        auditSteps.checkSubtaskCreated(createdSubtask.getId(),
                                       newTask.getId());
        querySteps.checkSubtaskHasParentTaskId(subtask.getId(),
                                               subtask.getParentTaskId());
    }

    @Then("a list of one subtask is be available for the task")
    public void getSubtasksForTask() {
        final Collection<CloudTask> subtasks = runtimeBundleSteps.getSubtasks(newTask.getId()).getContent();
        assertThat(subtasks).isNotNull().hasSize(1);
        assertThat(subtasks.iterator().hasNext()).isTrue();
        assertThat(subtasks.iterator().next()).extracting("id").containsOnly(subtask.getId());
    }
}
