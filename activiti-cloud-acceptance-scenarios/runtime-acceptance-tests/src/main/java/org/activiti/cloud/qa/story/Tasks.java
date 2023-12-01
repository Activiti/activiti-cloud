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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.awaitility.Awaitility.await;

import feign.FeignException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.steps.StepEventBus;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.audit.admin.AuditAdminSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.query.admin.TaskQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskVariableRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.TaskRuntimeAdminSteps;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.qa.helpers.VariableGenerator;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.PagedModel;

public class Tasks {

    public static final String STAND_ALONE_TASK_ID = "standAloneTaskId";

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;

    @Steps
    private TaskVariableRuntimeBundleSteps taskVariableRuntimeBundleSteps;

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
    private TaskQueryAdminSteps taskQueryAdminSteps;

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

    @Then("the created task has a status assigned")
    public void taskIsCreatedAndAssigned() throws Exception {
        final CloudTask assignedTask = taskRuntimeBundleSteps.getTaskById(newTask.getId());
        assertThat(assignedTask).isNotNull();
        assertThat(assignedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
        auditSteps.checkTaskCreatedAndAssignedEventsWhenAlreadyAssigned(assignedTask.getId());
        taskQuerySteps.checkTaskStatus(assignedTask.getId(), Task.TaskStatus.ASSIGNED);
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
        taskQuerySteps.checkTaskStatus(
            newTask.getId(),
            //TODO change to DELETED when RB is ready
            Task.TaskStatus.CANCELLED
        );
    }

    @When("user creates a subtask for the previously created task")
    public void createASubtask() {
        subtask = taskRuntimeBundleSteps.createSubtask(newTask.getId());
        assertThat(subtask).isNotNull();
        assertThat(subtask.getParentTaskId()).isNotNull();
        assertThat(subtask.getParentTaskId()).isNotEmpty();
    }

    @Then("the subtask is created and references another task")
    public void taskWithSubtaskIsCreated() {
        final CloudTask createdSubtask = taskRuntimeBundleSteps.getTaskById(subtask.getId());
        assertThat(createdSubtask).isNotNull();
        assertThat(createdSubtask.getParentTaskId()).isNotEmpty().isEqualToIgnoringCase(newTask.getId());
        auditSteps.checkSubtaskCreated(createdSubtask.getId(), newTask.getId());
        taskQuerySteps.checkSubtaskHasParentTaskId(subtask.getId(), subtask.getParentTaskId());
    }

    @Then("a list of one subtask is be available for the task")
    public void getSubtasksForTask() {
        final Collection<CloudTask> subtasks = taskRuntimeBundleSteps.getSubtasks(newTask.getId());
        assertThat(subtasks).isNotNull().extracting(CloudTask::getId).containsExactly(subtask.getId());
    }

    @Then("the task has the formKey field and correct processInstance fields")
    public void checkIfFormKeyAndProcessInstanceFieldsArePresent() {
        newTask = obtainFirstTaskFromProcess();
        assertThat(newTask).isNotNull().extracting(CloudTask::getFormKey).isEqualTo("taskForm");

        CloudProcessInstance processFromQuery = processQuerySteps.getProcessInstance(
            Serenity.sessionVariableCalled("processInstanceId").toString()
        );
        assertThat(processFromQuery).isNotNull();

        CloudTask taskFromRB = taskRuntimeBundleSteps.getTaskById(newTask.getId());
        assertThat(taskFromRB).isNotNull();
        assertThat(taskFromRB.getFormKey()).isEqualTo("taskForm");
        assertThat(taskFromRB.getProcessDefinitionId()).isEqualTo(processFromQuery.getProcessDefinitionId());

        CloudTask taskFromQuery = taskQuerySteps.getTaskById(newTask.getId());
        assertThat(taskFromQuery).isNotNull();
        assertThat(taskFromQuery.getFormKey()).isEqualTo("taskForm");
        assertThat(taskFromQuery.getProcessDefinitionId()).isEqualTo(processFromQuery.getProcessDefinitionId());
        assertThat(taskFromQuery.getProcessDefinitionVersion())
            .isEqualTo(processFromQuery.getProcessDefinitionVersion());
    }

    private CloudTask obtainFirstTaskFromProcess() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId").toString();
        List<CloudTask> tasksFromRB = new ArrayList<>(
            processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstanceId)
        );
        assertThat(tasksFromRB).isNotEmpty();
        newTask = tasksFromRB.get(0);
        assertThat(newTask).isNotNull();
        return newTask;
    }

    @Then("a task variable was created with name $variableName")
    @When("a task variable was created with name $variableName")
    public void verifyVariableCreated(String variableName) throws Exception {
        newTask = obtainFirstTaskFromProcess();

        taskQuerySteps.checkTaskHasVariable(newTask.getId(), variableName, variableName);

        auditSteps.checkTaskVariableEvent(
            Serenity.sessionVariableCalled("processInstanceId").toString(),
            newTask.getId(),
            variableName,
            VariableEvent.VariableEvents.VARIABLE_CREATED
        );
    }

    @Then("task variable $variableName has value $variableValue")
    @When("task variable $variableName has value $variableValue")
    public void verifyVariableValue(String variableName, String variableValue) throws Exception {
        newTask = obtainFirstTaskFromProcess();

        taskQuerySteps.checkTaskHasVariable(newTask.getId(), variableName, variableValue);
    }

    @Then("we update task variable $variableName to $variableValue")
    @When("we update task variable $variableName to $variableValue")
    public void updateTaskVariableValue(String variableName, String variableValue) {
        newTask = obtainFirstTaskFromProcess();

        taskVariableRuntimeBundleSteps.updateVariable(newTask.getId(), variableName, variableValue);
    }

    @When("the user updates the name of the task to $newTaskName")
    public void setTaskName(String newTaskName) {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        Collection<CloudTask> tasksCollection = taskQuerySteps
            .getTasksByProcessInstance(processInstanceId)
            .getContent();
        List<CloudTask> tasksList = new ArrayList<>(tasksCollection);
        newTask = tasksList.get(0);
        taskRuntimeBundleSteps.setTaskName(newTask.getId(), newTaskName);
    }

    @When("the user updates the updatable fields of the task")
    public void updateTaskFields() {
        getTaskToUpdateForCurrentProcessInstance();

        Date tomorrow = new Date(System.currentTimeMillis() + 86400000);
        Serenity.setSessionVariable("tomorrow").to(tomorrow);

        taskRuntimeBundleSteps.setTaskName(newTask.getId(), "new-task-name");
        taskRuntimeBundleSteps.setTaskPriority(newTask.getId(), 3);
        taskRuntimeBundleSteps.setTaskDueDate(newTask.getId(), tomorrow);
        taskRuntimeBundleSteps.setTaskFormKey(newTask.getId(), "new-task-form-key");
    }

    private void getTaskToUpdateForCurrentProcessInstance() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        waitForTasks(processInstanceId);
        Collection<CloudTask> tasksCollection = taskQuerySteps
            .getTasksByProcessInstance(processInstanceId)
            .getContent();
        List<CloudTask> tasksList = new ArrayList<>(tasksCollection);
        newTask = tasksList.get(0);
    }

    private void adminGetTaskToUpdateForCurrentProcessInstance() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        adminWaitForTasks(processInstanceId);
        Collection<CloudTask> tasksCollection = taskQueryAdminSteps
            .getTasksByProcessInstance(processInstanceId)
            .getContent();
        List<CloudTask> tasksList = new ArrayList<>(tasksCollection);
        newTask = tasksList.get(0);
    }

    private void waitForTasks(String processInstanceId) {
        await()
            .untilAsserted(() -> {
                Collection<CloudTask> tasksCollection = taskQuerySteps
                    .getTasksByProcessInstance(processInstanceId)
                    .getContent();
                assertThat(tasksCollection).isNotEmpty();
            });
    }

    private void adminWaitForTasks(String processInstanceId) {
        await()
            .untilAsserted(() -> {
                Collection<CloudTask> tasksCollection = taskQueryAdminSteps
                    .getTasksByProcessInstance(processInstanceId)
                    .getContent();
                assertThat(tasksCollection).isNotEmpty();
            });
    }

    @When("the admin updates the updatable fields of the task")
    public void adminUpdateTaskFields() {
        adminGetTaskToUpdateForCurrentProcessInstance();

        Date tomorrow = new Date(System.currentTimeMillis() + 86400000);
        Serenity.setSessionVariable("tomorrow").to(tomorrow);

        taskRuntimeAdminSteps.updateTask(
            newTask.getId(),
            TaskPayloadBuilder
                .update()
                .withName("new-task-name")
                .withPriority(3)
                .withDueDate(tomorrow)
                .withFormKey("new-task-form-key")
                .build()
        );
    }

    @Then("the task has the updated fields")
    public void checkUpdatedTaskFields() {
        Date tomorrow = Serenity.sessionVariableCalled("tomorrow");

        //name
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getName()).isEqualTo("new-task-name");
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getName()).isEqualTo("new-task-name");
        //priority
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getPriority()).isEqualTo(3);
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getPriority()).isEqualTo(3);
        //dueDate
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getDueDate()).isEqualTo(tomorrow);
        assertThat(taskQuerySteps.getTaskById(newTask.getId()).getDueDate()).isEqualTo(tomorrow);
        //formKey
        assertThat(taskRuntimeBundleSteps.getTaskById(newTask.getId()).getFormKey()).isEqualTo("new-task-form-key");
        //assertThat(taskQuerySteps.getTaskById(newTask.getId()).getFormKey()).isEqualTo("new-task-form-key");
    }

    @Then("the task is updated")
    public void checkIfTaskUpdated() {
        auditSteps.checkTaskUpdatedEvent(newTask.getId());
    }

    @Then("the user will get only root tasks when quering for root tasks")
    public void checkRootTasks() {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        await()
            .untilAsserted(() -> {
                Collection<CloudTask> rootTasksCollection = taskQuerySteps
                    .getRootTasksByProcessInstance(processInstanceId)
                    .getContent();

                assertThat(rootTasksCollection).isNotEmpty();

                rootTasksCollection.forEach(task -> assertThat(task.getParentTaskId()).isNull());
            });
    }

    @Then("the user will get only standalone tasks when quering for standalone tasks")
    public void checkStandaloneTasks() {
        Collection<CloudTask> standaloneTasksCollection = taskQuerySteps.getStandaloneTasks().getContent();

        assertThat(standaloneTasksCollection).isNotNull();
        assertThat(standaloneTasksCollection).isNotEmpty();
        standaloneTasksCollection.forEach(task -> assertThat(task.isStandalone()).isTrue());
    }

    @Then("the standalone task can be queried using LIKE operator")
    public void queryNameAndDescriptionWithLikeOperator() {
        PagedModel<CloudTask> retrievedTasks = taskQuerySteps.getTasksByNameAndDescription(
            newTask.getName().substring(0, 2),
            newTask.getDescription().substring(0, 2)
        );

        for (CloudTask task : retrievedTasks) {
            assertThat(task.getName()).contains(newTask.getName().substring(0, 2));
            assertThat(task.getDescription()).contains(newTask.getDescription().substring(0, 2));
        }
    }

    @When("the user creates task variables")
    public void setTaskVariables() {
        for (Map.Entry<String, Object> entry : VariableGenerator.variables.entrySet()) {
            taskVariableRuntimeBundleSteps.createVariable(newTask.getId(), entry.getKey(), entry.getValue());
            taskVariableRuntimeBundleSteps.getVariables(newTask.getId());
        }
    }

    @Then("task variables are visible in rb and query")
    public void checkTaskVariablesAreTheSameInRBAndQuery() {
        Map<String, Object> generatedMapRuntime = new HashMap<>();
        Map<String, Object> generatedMapQuery = new HashMap<>();

        taskVariableRuntimeBundleSteps
            .getVariables(newTask.getId())
            .forEach(element -> generatedMapRuntime.put(element.getName(), element.getValue()));

        taskQuerySteps
            .getVariables(newTask.getId())
            .forEach(element -> generatedMapQuery.put(element.getName(), element.getValue()));

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
    public void checkTaskStatusInRBAndQuery(Task.TaskStatus taskStatus) {
        taskRuntimeBundleSteps.checkTaskStatus(newTask.getId(), taskStatus);
        taskQuerySteps.checkTaskStatus(newTask.getId(), taskStatus);
    }

    @Then("the user is able to delete all tasks in query service")
    public void deleteAllTasksQuery() {
        //check standalone tasks
        assertThat(taskQueryAdminSteps.getAllTasks()).isNotEmpty();
        taskQueryAdminSteps.deleteTasks();
        assertThat(taskQueryAdminSteps.getAllTasks()).isEmpty();
    }

    @Then("the user retrieves the tasks and the standalone tasks separately")
    public void getTasksAndStandaloneTasks() {
        Collection<CloudTask> tasks = taskRuntimeBundleSteps.getAllTasks();
        assertThat(tasks).isNotEmpty();
        assertThat(tasks).isNotNull();

        Collection<CloudTask> standaloneTasks = taskRuntimeBundleSteps.getTaskWithStandalone(true);
        Collection<CloudTask> normalTasks = taskRuntimeBundleSteps.getTaskWithStandalone(false);

        assertThat(tasks.size()).isEqualTo(standaloneTasks.size() + normalTasks.size());

        normalTasks.forEach(cloudTask -> assertThat(tasks.contains(cloudTask)).isTrue());
        standaloneTasks.forEach(cloudTask -> assertThat(tasks.contains(cloudTask)).isTrue());
    }

    @Then("the status of the task is $taskStatus in Audit and Query")
    public void checkTaskStatusInAuditAndQuery(Task.TaskStatus taskStatus) throws Exception {
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId").toString();
        String currentTaskId = Serenity.sessionVariableCalled("currentTaskId").toString();

        auditSteps.checkProcessInstanceTaskEvent(
            processInstanceId,
            currentTaskId,
            TaskRuntimeEvent.TaskEvents.TASK_COMPLETED
        );

        taskQuerySteps.checkTaskStatus(currentTaskId, taskStatus);
    }

    @When("the task contains candidate groups $candidateGroups in Query")
    @Then("the task contains candidate groups $candidateGroups in Query")
    public void checkTaskCandidateGroups(String candidateGroups) {
        String currentTaskId = Serenity.sessionVariableCalled("currentTaskId").toString();
        waitForTask(currentTaskId);
        List<String> taskCandidateGroups = taskQuerySteps.getCandidateGroups(currentTaskId);

        assertThat(taskCandidateGroups).contains(candidateGroups.split(","));
    }

    @When("the task contains candidate users $user in Query")
    public void checkTaskCandidateUsers(String user) {
        String currentTaskId = Serenity.sessionVariableCalled("currentTaskId").toString();
        waitForTask(currentTaskId);
        List<String> taskCandidateUsers = taskQuerySteps.getCandidateUsers(currentTaskId);

        assertThat(taskCandidateUsers).contains(user);
    }

    private void waitForTask(String currentTaskId) {
        await()
            .untilAsserted(() -> {
                final Throwable throwable = catchThrowableOfType(
                    () -> taskQuerySteps.getTaskById(currentTaskId),
                    FeignException.class
                );
                if (throwable != null) {
                    //It's important to clear step failures after an Exception, otherwise,
                    //the step will be marked to be skipped and any subsequent call to
                    //taskQuerySteps will return mocks instead of calling the real endpoint.
                    //Without clearing step failures the await block become useless.
                    StepEventBus.getEventBus().clearStepFailures();
                }
                assertThat(throwable).isNull();
            });
    }

    @When("the task does not contain candidate user $user in Query")
    public void checkUserIsNotCandidate(String user) {
        String currentTaskId = Serenity.sessionVariableCalled("currentTaskId").toString();
        waitForTask(currentTaskId);
        List<String> taskCandidateUsers = taskQuerySteps.getCandidateUsers(currentTaskId);

        assertThat(taskCandidateUsers).doesNotContain(user);
    }
}
