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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.audit.admin.AuditAdminSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.activiti.cloud.api.task.model.CloudTask;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.assertThat;

public class Tasks {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;
    @Steps
    private ProcessRuntimeAdminSteps processRuntimeAdminSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;
    @Steps
    private TaskQuerySteps taskQuerySteps;
    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;
    @Steps
    private AuditAdminSteps auditAdminSteps;

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
        newTask = taskRuntimeBundleSteps.createNewTask();
        assertThat(newTask).isNotNull();
    }

    @Then("the task is created and the status is assigned")
    public void taskIsCreatedAndAssigned() throws Exception {
        final CloudTask assignedTask = taskRuntimeBundleSteps.getTaskById(newTask.getId());
        assertThat(assignedTask).isNotNull();
        assertThat(assignedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
        auditSteps.checkTaskCreatedAndAssignedEventsWhenAlreadyAssinged(assignedTask.getId());
        taskQuerySteps.checkTaskStatus(assignedTask.getId(),
                                   Task.TaskStatus.ASSIGNED);
    }

    @When("the user deletes the standalone task")
    public void deleteCurrentTask() {
        taskRuntimeBundleSteps.deleteTask(newTask.getId());
    }

    @Then("the standalone task is deleted")
    public void checkTaskIsDeleted() throws Exception {
        taskRuntimeBundleSteps.checkTaskNotFound(newTask.getId());
        auditSteps.checkTaskDeletedEvent(newTask.getId());
        taskQuerySteps.checkTaskStatus(newTask.getId(),
                                   //TODO change to DELETED when RB is ready
                                   Task.TaskStatus.CANCELLED);
    }

    @When("user creates a subtask for the previously created task")
    public void createASubtask() throws Exception {
        subtask = taskRuntimeBundleSteps.createSubtask(newTask.getId());
        assertThat(subtask).isNotNull();
        assertThat(subtask.getParentTaskId()).isNotNull();
        assertThat(subtask.getParentTaskId()).isNotEmpty();
    }

    @Then("the subtask is created and references another task")
    public void taskWithSubtaskIsCreated()  throws Exception {
        final CloudTask createdSubtask = taskRuntimeBundleSteps.getTaskById(subtask.getId());
        assertThat(createdSubtask).isNotNull();
        assertThat(createdSubtask.getParentTaskId()).isNotEmpty().isEqualToIgnoringCase(newTask.getId());
        auditSteps.checkSubtaskCreated(createdSubtask.getId(),
                                       newTask.getId());
        taskQuerySteps.checkSubtaskHasParentTaskId(subtask.getId(),
                                               subtask.getParentTaskId());
    }

    @Then("a list of one subtask is be available for the task")
    public void getSubtasksForTask() {
        final Collection<CloudTask> subtasks = taskRuntimeBundleSteps.getSubtasks(newTask.getId()).getContent();
        assertThat(subtasks).isNotNull().hasSize(1);
        assertThat(subtasks.iterator().hasNext()).isTrue();
        assertThat(subtasks.iterator().next()).extracting("id").containsOnly(subtask.getId());
    }

    @Then("the tasks has the formKey field")
    public void checkIfFormKeyIsPresent(){
        newTask = obtainFirstTaskFromProcess();
        assertThat(newTask).extracting("formKey").contains("taskForm");

        CloudTask taskFromQuery = taskRuntimeBundleSteps.getTaskById(newTask.getId());
        assertThat(taskFromQuery).isNotNull();
        assertThat(taskFromQuery.getFormKey()).isEqualTo("taskForm");
    }

    private CloudTask obtainFirstTaskFromProcess() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId").toString();
        List<CloudTask> tasksFromRB = new ArrayList<>(
                processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstanceId));
        assertThat(tasksFromRB).isNotEmpty();
        newTask = tasksFromRB.get(0);
        assertThat(newTask).isNotNull();
        return newTask;
    }

    @Then("a task variable was created with name $variableName")
    @When("a task variable was created with name $variableName")
    public void verifyVariableCreated(String variableName) throws Exception {
        newTask = obtainFirstTaskFromProcess();

        taskQuerySteps.checkTaskHasVariable(newTask.getId(),variableName,variableName);

        auditSteps.checkTaskVariableEvent(Serenity.sessionVariableCalled("processInstanceId").toString(),newTask.getId(), variableName, VariableEvent.VariableEvents.VARIABLE_CREATED);

    }

    @Then("task variable $variableName has value $variableValue")
    @When("task variable $variableName has value $variableValue")
    public void verifyVariableValue(String variableName, String variableValue) throws Exception {
        newTask = obtainFirstTaskFromProcess();

        taskQuerySteps.checkTaskHasVariable(newTask.getId(),variableName,variableValue);

    }

    @Then("we set task variable $variableName to $variableValue")
    @When("we set task variable $variableName to $variableValue")
    public void setTaskVariableValue(String variableName, String variableValue) throws Exception {
        newTask = obtainFirstTaskFromProcess();

        taskRuntimeBundleSteps.setVariables(newTask.getId(), Collections.singletonMap(variableName,variableValue));

    }

    @When("the user updates the name of the task to $newTaskName")
    public void setTaskName(String newTaskName){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Collection <CloudTask> tasksCollection = taskQuerySteps.getTasksByProcessInstance(processInstanceId).getContent();
        List <CloudTask> tasksList = new ArrayList(tasksCollection);
        newTask = tasksList.get(0);
        taskRuntimeBundleSteps.setTaskName(newTask.getId(), newTaskName);
    }

    @Then("the task has the name $newTaskName")
    public void checkTaskName (String newTaskName){
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getName())
                .isEqualTo(newTaskName);
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getName())
                .isEqualTo(newTaskName);
    }

    @Then("the task is updated")
    public void checkIfTaskUpdated (){
        auditSteps.checkTaskUpdatedEvent(newTask.getId());
    }
}
