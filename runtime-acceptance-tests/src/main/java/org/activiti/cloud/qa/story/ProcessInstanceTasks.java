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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.audit.admin.AuditAdminSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.activiti.cloud.acc.shared.rest.error.ExpectRestNotFound;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.acc.core.helper.Filters.checkEvents;
import static org.activiti.cloud.acc.core.helper.Filters.checkProcessInstances;
import static org.activiti.cloud.acc.core.helper.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.activiti.cloud.acc.core.helper.ProcessDefinitionRegistry.withTasks;
import static org.activiti.cloud.acc.core.helper.ProcessDefinitionRegistry.processDefinitionKeys;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessInstanceTasks {

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

    private ProcessInstance processInstance;

    private String processInstanceDiagram;

    private Task currentTask;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        taskRuntimeBundleSteps.checkServicesHealth();
        processRuntimeAdminSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        taskQuerySteps.checkServicesHealth();
        processQueryAdminSteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
        auditAdminSteps.checkServicesHealth();
    }

    @When("the user starts with variables for $processName with variables $variableName1 and $variableName2")
    public void startProcess(String processName, String variableName1, String variableName2) {
        Map<String,Object> variables = new HashMap<>();
        variables.put(variableName1,variableName1);  //using var names as values
        variables.put(variableName2,variableName2);

        processInstance = processRuntimeBundleSteps.startProcessWithVariables(processDefinitionKeyMatcher(processName),variables);
        checkProcessWithTaskCreated(processName);
    }

    @When("the user starts a $processName")
    public void startProcess(String processName) {

        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName));
        checkProcessWithTaskCreated(processName);
    }

    private void checkProcessWithTaskCreated(String processName) {
        assertThat(processInstance).isNotNull();

        if(withTasks(processName)){
            List<Task> tasks = new ArrayList<>(
                    processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId()));
            assertThat(tasks).isNotEmpty();
            currentTask = tasks.get(0);
            assertThat(currentTask).isNotNull();
        }

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Given("any suspended process instance")
    public void suspendCurrentProcessInstance() {
        this.startProcess("SIMPLE_PROCESS_INSTANCE");
        processRuntimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

    @When("the assignee is $user")
    public void checkAssignee(String user)throws Exception {
        assertThat(taskRuntimeBundleSteps.getTaskById(currentTask.getId()).getAssignee()).isEqualTo(user);
    }

    @When("the user claims the task")
    public void claimTask() throws Exception {
        taskRuntimeBundleSteps.claimTask(currentTask.getId());
    }

    @When("the user completes the task")
    public void completeTask() throws Exception {
        taskRuntimeBundleSteps.completeTask(currentTask.getId(),
                TaskPayloadBuilder
                        .complete()
                        .withTaskId(currentTask.getId())
                        .build());
    }

    @Then("the user cannot complete the task")
    public void cannotCompleteTask() throws Exception {
        taskRuntimeBundleSteps.cannotCompleteTask(currentTask.getId(),
                TaskPayloadBuilder
                        .complete()
                        .withTaskId(currentTask.getId())
                        .build());
    }

    @When("the status of the task since the beginning is $status")
    public void checkTaskStatusSinceBeginning(Task.TaskStatus status){
        taskRuntimeBundleSteps.checkTaskStatus(currentTask.getId(), status);
        taskQuerySteps.checkTaskStatus(currentTask.getId(), status);
        auditSteps.checkTaskCreatedAndAssignedEventsWhenAlreadyAssinged(currentTask.getId());
    }

    @When("the status of the task is $status")
    public void checkTaskStatus(Task.TaskStatus status) throws Exception {
        taskRuntimeBundleSteps.checkTaskStatus(currentTask.getId(), status);
        taskQuerySteps.checkTaskStatus(currentTask.getId(), status);

        switch (status){
            case CREATED:
                auditSteps.checkTaskCreatedEvent(currentTask.getId());
                break;
            case ASSIGNED:
                auditSteps.checkTaskCreatedAndAssignedEvents(currentTask.getId());
                break;
            case COMPLETED:
                auditSteps.checkTaskCreatedAndAssignedAndCompletedEvents(currentTask.getId());
                break;
        }

    }

    @Then("the task cannot be claimed by user")
    public void cannotClaimTask() throws Exception {
        taskRuntimeBundleSteps.cannotClaimTask(currentTask.getId());
        //the claimed task shouldn't be found by query
        Collection <? extends Task> tasks = taskQuerySteps.getAllTasks().getContent();
        assertThat(tasks).extracting("id").doesNotContain(currentTask.getId());
    }

    @Then("tasks of $processName cannot be seen by user")
    public void cannotSeeTasksOfDefinition(String processName) throws Exception {
        Collection <? extends Task> tasks = taskQuerySteps.getAllTasks().getContent();
        assertThat(tasks).extracting("processDefinitionId").doesNotContain(processDefinitionKeyMatcher(processName));
    }

    @Then("the status of the process and the task is changed to completed")
    public void verifyProcessAndTasksStatus() throws Exception {

        processQuerySteps.checkProcessInstanceStatus(processInstance.getId(),
                ProcessInstance.ProcessInstanceStatus.COMPLETED);
        auditSteps.checkProcessInstanceTaskEvent(processInstance.getId(),
                                                 currentTask.getId(),
                                                 TaskRuntimeEvent.TaskEvents.TASK_COMPLETED);
        //the process instance disappears once it is completed
        processRuntimeBundleSteps.checkProcessInstanceIsNotPresent(processInstance.getId());

    }

    @Then("the status of the process is changed to completed")
    public void verifyProcessStatus() throws Exception {

        processQuerySteps.checkProcessInstanceStatus(processInstance.getId(),
                ProcessInstance.ProcessInstanceStatus.COMPLETED);
        auditSteps.checkProcessInstanceEvent(processInstance.getId(), ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED);
    }

    @Then("a variable was created with name $variableName")
    @When("a variable was created with name $variableName")
    public void verifyVariableCreated(String variableName) throws Exception {

        processQuerySteps.checkProcessInstanceHasVariable(processInstance.getId(),
                variableName);
        auditSteps.checkProcessInstanceVariableEvent(processInstance.getId(),
                variableName,
                VariableEvent.VariableEvents.VARIABLE_CREATED);

    }

    @When("the user deletes the process")
    public void deleteCurrentProcessInstance() throws Exception {
        processRuntimeBundleSteps.deleteProcessInstance(processInstance.getId());
    }

    @Then("the process instance is deleted")
    public void verifyProcessInstanceIsDeleted() throws Exception {
        //TODO change to DELETED status and PROCESS_DELETED event when RB is ready
        processRuntimeBundleSteps.checkProcessInstanceNotFound(processInstance.getId());
        processQuerySteps.checkProcessInstanceStatus(processInstance.getId(),
                ProcessInstance.ProcessInstanceStatus.CANCELLED);
        auditSteps.checkProcessInstanceEvent(processInstance.getId(),
                                             ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED);
    }

    @When("open the process diagram")
    public void openProcessInstanceDiagram() {
        processInstanceDiagram = processRuntimeBundleSteps.openProcessInstanceDiagram(processInstance.getId());
    }

    @Then("the diagram is shown")
    public void checkProcessInstanceDiagram() throws Exception {
        processRuntimeBundleSteps.checkProcessInstanceDiagram(processInstanceDiagram);
    }

    @Then("no diagram is shown")
    public void checkProcessInstanceNoDiagram() throws Exception {
        processRuntimeBundleSteps.checkProcessInstanceNoDiagram(processInstanceDiagram);
    }

    @When("activate the process")
    public void activateCurrentProcessInstance() {
        processRuntimeBundleSteps.resumeProcessInstance(processInstance.getId());
    }

    @Then("the process cannot be activated anymore")
    @ExpectRestNotFound("Unable to find process instance for the given id")
    public void cannotActivateProcessInstance() {
        processRuntimeBundleSteps.resumeProcessInstance(processInstance.getId());
    }

    @Then("the user can get events for process with variables instances in admin endpoint")
    public void checkIfEventsFromProcessesWithVariablesArePresentAdmin(){
        //TODO some refactoring after fixing the behavior of the /admin/v1/events?search=entityId:UUID endpoint
        Collection<CloudRuntimeEvent> filteredCollection = checkEvents(auditAdminSteps.getEventsByEntityIdAdmin(Serenity.sessionVariableCalled("processInstanceId")), processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"));
        assertThat(filteredCollection).isNotEmpty();
        assertThat(((ProcessInstanceImpl)filteredCollection.iterator().next().getEntity()).getProcessDefinitionKey()).isEqualTo(processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"));
    }

    @Then("the user can query process with variables instances in admin endpoints")
    public void checkIfProcessWithVariablesArePresentQueryAdmin(){
        assertThat(checkProcessInstances(processQueryAdminSteps.getAllProcessInstancesAdmin(),processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isNotEmpty();
    }

    @Then("the user can get process with variables instances in admin endpoint")
    public void checkIfProcessWithVariablesArePresentAdmin(){
        assertThat(checkProcessInstances(processRuntimeAdminSteps.getProcessInstances(), processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isNotEmpty();
    }

    @Then("the task from $processName is $status and it is called $taskName")
    public void checkTaskFromProcessInstance(String processName,Task.TaskStatus status, String taskName){
        List<ProcessInstance> processInstancesList = new ArrayList<>(
                processRuntimeBundleSteps.getAllProcessInstances().getContent());
        assertThat(processInstancesList).extracting("processDefinitionKey")
                .contains(processDefinitionKeyMatcher(processName));

        //filter the list
        processInstancesList = processInstancesList.stream().filter(p -> p.getProcessDefinitionKey().equals(processDefinitionKeyMatcher(processName))).collect(Collectors.toList());
        assertThat(processInstancesList.size()).isEqualTo(1);

        List<Task> tasksList = new ArrayList<>(processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstancesList.get(0).getId()));

        assertThat(tasksList).isNotEmpty();
        currentTask = tasksList.get(0);
        assertThat(currentTask.getStatus()).isEqualTo(status);
        assertThat(currentTask.getName()).isEqualTo(taskName);
    }

    @When("the user gets the process definitions")
    public void getProcessDefinitions(){
        Collection<ProcessDefinition> processDefinitions = processRuntimeBundleSteps.getProcessDefinitions().getContent();
        Serenity.setSessionVariable("processDefinitions").to(processDefinitions);
    }

    @Then("all the process definitions are present")
    public void checkProcessDefinitions(){
        Collection<ProcessDefinition> processDefinitions = Serenity.sessionVariableCalled("processDefinitions");
        assertThat(processDefinitions)
                .extracting("key")
                .containsAll(processDefinitionKeys.values());
    }

    @Then("the $processName definition has the $field field with value $value")
    public void checkIfFieldIsPresentAndHasValue(String processName, String field, String value){
        ProcessDefinition processDefinition = processRuntimeBundleSteps
                .getProcessDefinitionByKey(processDefinitionKeyMatcher(processName));
        assertThat(processDefinition)
                .extracting(field)
                .contains(value);
    }
}
