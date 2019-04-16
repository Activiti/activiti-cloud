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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.audit.admin.AuditAdminSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.TaskRuntimeAdminSteps;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.qa.helpers.VariableGenerator;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.PagedResources;

import static org.assertj.core.api.Assertions.assertThat;

public class Tasks {

    public static final String STAND_ALONE_TASK_ID = "standAloneTaskId";

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;
    @Steps
    private TaskRuntimeAdminSteps taskRuntimeAdminSteps;
    
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
        Serenity.setSessionVariable(STAND_ALONE_TASK_ID).to(newTask.getId());
    }

    @When("the user creates an unassigned standalone task")
    public void createUnassignedTask() throws Exception {
        newTask = taskRuntimeBundleSteps.createNewUnassignedTask();
        assertThat(newTask).isNotNull();
        Serenity.setSessionVariable(STAND_ALONE_TASK_ID).to(newTask.getId());
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
    
    @When("the admin deletes the standalone task")
    public void adminDeleteCurrentTask() {
        taskRuntimeAdminSteps.deleteTask(newTask.getId());
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

    @Then("the task has the formKey field and correct processInstance fields")
    public void checkIfFormKeyAndProcessInstanceFieldsArePresent(){
        newTask = obtainFirstTaskFromProcess();
        assertThat(newTask).extracting("formKey").contains("taskForm");

        CloudProcessInstance processFromQuery = processQuerySteps.getProcessInstance(Serenity.sessionVariableCalled("processInstanceId").toString());
        assertThat(processFromQuery).isNotNull();
      
        CloudTask taskFromRB = taskRuntimeBundleSteps.getTaskById(newTask.getId());
        assertThat(taskFromRB).isNotNull();
        assertThat(taskFromRB.getFormKey()).isEqualTo("taskForm");
        assertThat(taskFromRB.getProcessDefinitionId()).isEqualTo(processFromQuery.getProcessDefinitionId());
        
        CloudTask taskFromQuery = taskQuerySteps.getTaskById(newTask.getId());
        assertThat(taskFromQuery).isNotNull();
        assertThat(taskFromQuery.getFormKey()).isEqualTo("taskForm");
        assertThat(taskFromQuery.getProcessDefinitionId()).isEqualTo(processFromQuery.getProcessDefinitionId());
        assertThat(taskFromQuery.getProcessDefinitionVersion()).isEqualTo(processFromQuery.getProcessDefinitionVersion());
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

    @Then("we update task variable $variableName to $variableValue")
    @When("we update task variable $variableName to $variableValue")
    public void updateTaskVariableValue(String variableName, String variableValue) {
        newTask = obtainFirstTaskFromProcess();

        taskRuntimeBundleSteps.updateVariable(newTask.getId(),
                                              variableName,
                                              variableValue);

    }

    @When("the user updates the name of the task to $newTaskName")
    public void setTaskName(String newTaskName){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Collection <CloudTask> tasksCollection = taskQuerySteps.getTasksByProcessInstance(processInstanceId).getContent();
        List <CloudTask> tasksList = new ArrayList<>(tasksCollection);
        newTask = tasksList.get(0);
        taskRuntimeBundleSteps.setTaskName(newTask.getId(), newTaskName);
    }

    @When("the user updates the updatable fields of the task")
    public void updateTaskFields(){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Collection <CloudTask> tasksCollection = taskQuerySteps.getTasksByProcessInstance(processInstanceId).getContent();
        List <CloudTask> tasksList = new ArrayList<>(tasksCollection);
        newTask = tasksList.get(0);

        Date tomorrow = new Date(System.currentTimeMillis() + 86400000);
        Serenity.setSessionVariable("tomorrow").to(tomorrow);

        taskRuntimeBundleSteps.setTaskName(newTask.getId(), "new-task-name");
        taskRuntimeBundleSteps.setTaskPriority(newTask.getId(), 3);
        taskRuntimeBundleSteps.setTaskDueDate(newTask.getId(),tomorrow);
        taskRuntimeBundleSteps.setTaskFormKey(newTask.getId(), "new-task-form-key");
    }

    @When("the admin updates the updatable fields of the task")
    public void adminUpdateTaskFields(){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Collection <CloudTask> tasksCollection = taskQuerySteps.getTasksByProcessInstance(processInstanceId).getContent();
        List <CloudTask> tasksList = new ArrayList<>(tasksCollection);
        newTask = tasksList.get(0);

        Date tomorrow = new Date(System.currentTimeMillis() + 86400000);
        Serenity.setSessionVariable("tomorrow").to(tomorrow);

        taskRuntimeAdminSteps.updateTask(newTask.getId(), 
                                         TaskPayloadBuilder
                                         .update()
                                         .withName("new-task-name")
                                         .withPriority(3)
                                         .withDueDate(tomorrow)
                                         .withFormKey("new-task-form-key")
                                         .build());
    }
    
    @Then("the task has the updated fields")
    public void checkUpdatedTaskFields (){
        Date tomorrow = Serenity.sessionVariableCalled("tomorrow");

        //name
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getName())
                .isEqualTo("new-task-name");
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getName())
                .isEqualTo("new-task-name");
        //priority
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getPriority())
                .isEqualTo(3);
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getPriority())
                .isEqualTo(3);
        //dueDate
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getDueDate())
                .isEqualTo(tomorrow);
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getDueDate())
                .isEqualTo(tomorrow);
        //formKey
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getFormKey())
                .isEqualTo("new-task-form-key");
        //assertThat(taskQuerySteps.getTaskById(newTask.getId()).getFormKey()).isEqualTo("new-task-form-key");
    }

    @Then("the task is updated")
    public void checkIfTaskUpdated (){
        auditSteps.checkTaskUpdatedEvent(newTask.getId());
    }
    
    @Then("the user will see only root tasks when quering for root tasks")
    public void checkRootTasks(){
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Collection <CloudTask> rootTasksCollection = taskQuerySteps.getRootTasksByProcessInstance(processInstanceId).getContent();

        assertThat(rootTasksCollection).isNotNull();
        assertThat(rootTasksCollection).isNotEmpty();
        
        rootTasksCollection.forEach(
                task -> assertThat(task.getParentTaskId()).isNull()
        );
    }
    
    @Then("the user will see only standalone tasks when quering for standalone tasks")
    public void checkStandaloneTasks(){
        Collection <CloudTask> standaloneTasksCollection = taskQuerySteps.getStandaloneTasks().getContent();

        assertThat(standaloneTasksCollection).isNotNull();
        assertThat(standaloneTasksCollection).isNotEmpty();
        standaloneTasksCollection.forEach(
                task -> assertThat(task.getProcessInstanceId()).isNull()
        );
    }

    @Then("the standalone task can be queried using LIKE operator")
    public void queryNameAndDescriptionWithLikeOperator(){

        PagedResources<CloudTask> retrievedTasks = taskQuerySteps.getTasksByNameAndDescription(newTask.getName().substring(0,2),
                newTask.getDescription().substring(0,2));

        for(CloudTask task : retrievedTasks){
            assertThat(task.getName()).contains(newTask.getName().substring(0,2));
            assertThat(task.getDescription()).contains(newTask.getDescription().substring(0,2));
        }
    }

    @When("the user creates task variables")
    public void setTaskVariables(){
        for (Map.Entry<String, Object> entry : VariableGenerator.variables.entrySet()) {
            taskRuntimeBundleSteps.createVariable(newTask.getId(), entry.getKey(), entry.getValue());
        }
    }

    @Then("task variables are visible in rb and query")
    public void checkTaskVariablesAreTheSameInRBAndQuery(){

        Map <String,Object> generatedMapRuntime = new HashMap<>();
        Map <String,Object> generatedMapQuery = new HashMap<>();

        taskRuntimeBundleSteps
                .getVariables(newTask.getId())
                .getContent()
                .stream()
                .forEach(element -> generatedMapRuntime.put(element.getName(),element.getValue()));

        taskQuerySteps
                .getVariables(newTask.getId())
                .getContent()
                .stream()
                .forEach(element -> generatedMapQuery.put(element.getName(),element.getValue()));

        assertThat(generatedMapRuntime).isEqualTo(VariableGenerator.variables);
        assertThat(generatedMapQuery).isEqualTo(VariableGenerator.variables);
    }

    @When("the user claims the standalone task")
    public void claimTask() throws Exception {
        taskRuntimeBundleSteps.claimTask(newTask.getId());
    }

    @When("the user releases the standalone task")
    public void releaseTask() throws Exception {
        taskRuntimeBundleSteps.releaseTask(newTask.getId());
    }

    @Then("the status of the task is $taskStatus in RB and Query")
    public void checkTaskStatusInRBAndQuery(Task.TaskStatus taskStatus){
        taskRuntimeBundleSteps.checkTaskStatus(newTask.getId(), taskStatus);
        taskQuerySteps.checkTaskStatus(newTask.getId(), taskStatus);
    }
}
