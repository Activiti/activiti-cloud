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
import java.util.List;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.qa.rest.error.ExpectRestNotFound;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.activiti.cloud.qa.helper.Filters.checkEvents;
import static org.activiti.cloud.qa.helper.Filters.checkProcessInstances;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.CONNECTOR_PROCESS_INSTANCE_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITH_SINGLE_TASK_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO_DEFINITION_KEY;
import static org.assertj.core.api.Assertions.assertThat;

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

    @When("the user starts a $processName")
    public void startProcess(String processName) {

        String processDefinitionKey;
        boolean withTasks = true;

        switch(processName){
            case "process with variables":
                processDefinitionKey = PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY;
                break;
            case "single-task process":
                processDefinitionKey = PROCESS_INSTANCE_WITH_SINGLE_TASK_DEFINITION_KEY;
                break;
            case "single-task process with user candidates":
                processDefinitionKey = PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES_DEFINITION_KEY;
                break;
            case "single-task process with group candidates":
                processDefinitionKey = PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_DEFINITION_KEY;
                break;
            case "process without graphic info":
                processDefinitionKey = PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO_DEFINITION_KEY;
                break;
            case "connector process":
                processDefinitionKey = CONNECTOR_PROCESS_INSTANCE_DEFINITION_KEY;
                withTasks = false;
                break;
            case "single-task process with group candidates for test group":
                processDefinitionKey = "singletask-b6095889-6177-4b73-b3d9-316e47749a36";
                break;
            default:
                processDefinitionKey = SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY;
                withTasks = false;
        }

        processInstance = runtimeBundleSteps.startProcess(processDefinitionKey);
        assertThat(processInstance).isNotNull();

        if(withTasks){
            List<Task> tasks = new ArrayList<>(
                    runtimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId()));
            assertThat(tasks).isNotEmpty();
            currentTask = tasks.get(0);
            assertThat(currentTask).isNotNull();
        }

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Given("any suspended process instance")
    public void suspendCurrentProcessInstance() {
        this.startProcess("any");
        runtimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

    @When("the assignee is $user")
    public void checkAssignee(String user)throws Exception {
        assertThat(runtimeBundleSteps.getTaskById(currentTask.getId()).getAssignee()).isEqualTo(user);
    }

    @When("the $user claims the task")
    public void claimTask(String user) throws Exception {
        runtimeBundleSteps.assignTaskToUser(currentTask.getId(),
                                            user);
    }

    @When("the user completes the task")
    public void completeTask() throws Exception {
        runtimeBundleSteps.completeTask(currentTask.getId());
    }

    @Then("the user cannot complete the task")
    public void cannotCompleteTask() throws Exception{
        runtimeBundleSteps.cannotCompleteTask(currentTask.getId());
    }

    @When("the status of the task since the beginning is $status")
    public void checkTaskStatusSinceBeginning(Task.TaskStatus status){
        querySteps.checkTaskStatus(currentTask.getId(), status);
        runtimeBundleSteps.checkTaskStatus(currentTask.getId(), status);
        auditSteps.checkTaskCreatedAndAssignedEventsWhenAlreadyAssinged(currentTask.getId());
    }

    @When("the status of the task is $status")
    public void checkTaskStatus(Task.TaskStatus status) throws Exception {
        querySteps.checkTaskStatus(currentTask.getId(), status);
        runtimeBundleSteps.checkTaskStatus(currentTask.getId(), status);

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

    @Then("the task cannot be claimed by $user")
    public void cannotClaimTask(String user) throws Exception {
        runtimeBundleSteps.cannotAssignTaskToUser(currentTask.getId(),
                                            user);
        //the claimed task shouldn't be found by query
        Collection <? extends Task> tasks = querySteps.getAllTasks().getContent();
        assertThat(tasks).extracting("id").doesNotContain(currentTask.getId());
    }

    @Then("tasks of $definitionKey cannot be seen by user")
    public void cannotSeeTasksOfDefinition(String defintionKey) throws Exception {
        Collection <? extends Task> tasks = querySteps.getAllTasks().getContent();
        assertThat(tasks).extracting("processDefinitionId").doesNotContain(defintionKey);
    }

    @Then("the status of the process and the task is changed to completed")
    public void verifyProcessAndTasksStatus() throws Exception {

        querySteps.checkProcessInstanceStatus(processInstance.getId(),
                                              ProcessInstance.ProcessInstanceStatus.COMPLETED);
        auditSteps.checkProcessInstanceTaskEvent(processInstance.getId(),
                                                 currentTask.getId(),
                                                 TaskRuntimeEvent.TaskEvents.TASK_COMPLETED);
        //the process instance disappears once it is completed
        runtimeBundleSteps.checkProcessInstanceIsNotPresent(processInstance.getId());

    }

    @Then("the status of the process is changed to completed")
    public void verifyProcessStatus() throws Exception {

        querySteps.checkProcessInstanceStatus(processInstance.getId(),
                ProcessInstance.ProcessInstanceStatus.COMPLETED);
        auditSteps.checkProcessInstanceEvent(processInstance.getId(), ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED);
    }

    @Then("a variable was created with name $variableName")
    public void verifyVariableCreated(String variableName) throws Exception {

        querySteps.checkProcessInstanceHasVariable(processInstance.getId(),variableName);

        auditSteps.checkProcessInstanceVariableEvent(processInstance.getId(), variableName, VariableEvent.VariableEvents.VARIABLE_CREATED);

    }

    @When("the user deletes the process")
    public void deleteCurrentProcessInstance() throws Exception {
        runtimeBundleSteps.deleteProcessInstance(processInstance.getId());
    }

    @Then("the process instance is deleted")
    public void verifyProcessInstanceIsDeleted() throws Exception {
        //TODO change to DELETED status and PROCESS_DELETED event when RB is ready
        runtimeBundleSteps.checkProcessInstanceNotFound(processInstance.getId());
        querySteps.checkProcessInstanceStatus(processInstance.getId(),
                                              ProcessInstance.ProcessInstanceStatus.CANCELLED);
        auditSteps.checkProcessInstanceEvent(processInstance.getId(),
                                             ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED);
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

    @When("activate the process")
    public void activateCurrentProcessInstance() {
        runtimeBundleSteps.activateProcessInstance(processInstance.getId());
    }

    @Then("the process cannot be activated anymore")
    @ExpectRestNotFound("Unable to find process instance for the given id")
    public void cannotActivateProcessInstance() {
        runtimeBundleSteps.activateProcessInstance(processInstance.getId());
    }

    @Then("the user can get events for process with variables instances in admin endpoint")
    public void checkIfEventsFromProcessesWithVariablesArePresentAdmin(){
        //TODO some refactoring after fixing the behavior of the /admin/v1/events?search=entityId:UUID endpoint
        Collection<CloudRuntimeEvent> filteredCollection = checkEvents(auditSteps.getEventsByEntityIdAdmin(Serenity.sessionVariableCalled("processInstanceId")), PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY);
        assertThat(filteredCollection).isNotEmpty();
        assertThat(((ProcessInstanceImpl)filteredCollection.iterator().next().getEntity()).getProcessDefinitionKey()).isEqualTo(PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY);
    }

    @Then("the user can query process with variables instances in admin endpoints")
    public void checkIfProcessWithVariablesArePresentQueryAdmin(){
        assertThat(checkProcessInstances(querySteps.getAllProcessInstancesAdmin(),PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user can get process with variables instances in admin endpoint")
    public void checkIfProcessWithVariablesArePresentAdmin(){
        assertThat(checkProcessInstances(runtimeBundleSteps.getAllProcessInstancesAdmin(), PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isNotEmpty();
    }
}
