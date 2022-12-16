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

import static org.activiti.cloud.acc.core.helper.Filters.checkProcessInstances;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeys;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.withTasks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import feign.FeignException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
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
import org.activiti.cloud.acc.core.steps.runtime.admin.TaskRuntimeAdminSteps;
import org.activiti.cloud.acc.shared.rest.error.ExpectRestNotFound;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.model.ExamplesTable;
import org.springframework.hateoas.PagedModel;

public class ProcessInstanceTasks {

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

    private ProcessInstance processInstance;

    private String processInstanceDiagram;

    private Task currentTask;
    private String processInstanceAdminDiagram;

    private static int AUDIT_STEP_TIMEOUT = 60;

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
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName1, variableName1); //using var names as values
        variables.put(variableName2, variableName2);

        processInstance =
            processRuntimeBundleSteps.startProcessWithVariables(processDefinitionKeyMatcher(processName), variables);
        checkProcessWithTaskCreated(processName);
    }

    @When("the user starts an instance of the process called $processName")
    public void startProcess(String processName) {
        String processDefinitionKey = processDefinitionKeyMatcher(processName);
        startProcessByKey(processDefinitionKey);
        checkProcessWithTaskCreated(processName);
    }

    @When("the user starts an instance of the process with key $processDefinitionKey")
    public void startProcessByKey(String processDefinitionKey) {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKey);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @When("the user starts a process with variables called $processName")
    public void startProcessWithVariables(String processName) throws IOException {
        String processDefinitionKey = processDefinitionKeyMatcher(processName);
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKey, true);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
        checkProcessWithTaskCreated(processName);
    }

    private void checkProcessWithTaskCreated(String processName) {
        assertThat(processInstance).isNotNull();

        if (withTasks(processName)) {
            List<Task> tasks = new ArrayList<>(
                processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId())
            );
            assertThat(tasks).isNotEmpty();
            currentTask = tasks.get(0);
            assertThat(currentTask).isNotNull();
            Serenity.setSessionVariable("currentTaskId").to(currentTask.getId());
        }

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Given("any suspended process instance")
    public void suspendCurrentProcessInstance() {
        this.startProcess("SIMPLE_PROCESS_INSTANCE");
        processRuntimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

    @When("the assignee is $user")
    @Then("the assignee is $user")
    public void checkAssignee(String user) throws Exception {
        assertThat(taskRuntimeBundleSteps.getTaskById(currentTask.getId()).getAssignee()).isEqualTo(user);
    }

    @When("the user claims the task")
    public void claimTask() throws Exception {
        taskRuntimeBundleSteps.claimTask(currentTask.getId());
    }

    @When("the user assign the task to $assignee")
    public void assignTask(String assignee) throws Exception {
        taskRuntimeBundleSteps.assignTask(
            currentTask.getId(),
            TaskPayloadBuilder.assign().withTaskId(currentTask.getId()).withAssignee(assignee).build()
        );
    }

    @Then("the user cannot assign the task to $user")
    public void cannotAssignTask(String user) throws Exception {
        taskRuntimeBundleSteps.cannotAssignTask(
            currentTask.getId(),
            TaskPayloadBuilder.assign().withTaskId(currentTask.getId()).withAssignee(user).build()
        );
    }

    @When("the user releases the task")
    public void releaseTask() throws Exception {
        taskRuntimeBundleSteps.releaseTask(currentTask.getId());
    }

    @When("the user completes the task")
    public void completeTask() throws Exception {
        taskRuntimeBundleSteps.completeTask(
            currentTask.getId(),
            TaskPayloadBuilder.complete().withTaskId(currentTask.getId()).build()
        );
    }

    @When("the user completes the task with variable $variableName set to $value")
    public void completeTask(String variableName, String value) throws Exception {
        taskRuntimeBundleSteps.completeTask(
            currentTask.getId(),
            TaskPayloadBuilder.complete().withTaskId(currentTask.getId()).withVariable(variableName, value).build()
        );
    }

    @Then("the process instance reaches a task named $taskName")
    public void checkProcessIsOnTask(String taskName) throws Exception {
        await()
            .untilAsserted(() -> {
                Collection<CloudTask> tasks = processRuntimeBundleSteps.getTaskByProcessInstanceId(
                    processInstance.getId()
                );
                assertThat(tasks).extracting(CloudTask::getName).contains("Wait");
            });
    }

    @Then(
        "the process instance has a resultCollection named $resultCollectionName with entries of size $entriesSize as following: $variableTable"
    )
    public void checkProcessHasResultCollection(
        String resultCollectionName,
        int entriesSize,
        ExamplesTable variableTable
    ) {
        List<Object> resultCollection = new ArrayList<>();
        Map<String, Object> resultCollectionEntry = new HashMap<>();
        Iterator<Map<String, String>> iterator = variableTable.getRows().iterator();
        int currentCount = 1;
        while (iterator.hasNext()) {
            Map<String, String> currentRow = iterator.next();
            resultCollectionEntry.put(currentRow.get("name"), currentRow.get("value"));
            if (currentCount % entriesSize == 0) {
                resultCollection.add(resultCollectionEntry);
                resultCollectionEntry = new HashMap<>();
            }
            currentCount++;
        }

        processQuerySteps.checkProcessInstanceHasVariableValue(
            processInstance.getId(),
            resultCollectionName,
            resultCollection
        );
    }

    @When(
        "the user completes the task available in the current process instance passing the following variables: $variableTable"
    )
    public void completeTask(ExamplesTable variableTable) {
        Map<String, Object> variables = new HashMap<>();
        variableTable.getRows().forEach(map -> variables.put(map.get("name"), map.get("value")));
        Collection<CloudTask> tasks = processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstance.getId());
        assertThat(tasks).isNotEmpty();
        taskRuntimeBundleSteps.completeTask(
            tasks.iterator().next().getId(),
            TaskPayloadBuilder.complete().withVariables(variables).build()
        );
    }

    @When("the admin completes the task")
    public void adminCompleteTask() throws Exception {
        taskRuntimeAdminSteps.completeTask(
            currentTask.getId(),
            TaskPayloadBuilder.complete().withTaskId(currentTask.getId()).build()
        );
    }

    @Then("the user cannot complete the task")
    public void cannotCompleteTask() throws Exception {
        taskRuntimeBundleSteps.cannotCompleteTask(
            currentTask.getId(),
            TaskPayloadBuilder.complete().withTaskId(currentTask.getId()).build()
        );
    }

    @When("the status of the task since the beginning is $status")
    public void checkTaskStatusSinceBeginning(Task.TaskStatus status) {
        taskRuntimeBundleSteps.checkTaskStatus(currentTask.getId(), status);
        taskQuerySteps.checkTaskStatus(currentTask.getId(), status);
        auditSteps.checkTaskCreatedAndAssignedEventsWhenAlreadyAssigned(currentTask.getId());
    }

    @When("the user saves the task with variable $variableName equal to $variableValue")
    public void saveTask(String variableName, String variableValue) throws Exception {
        taskRuntimeBundleSteps.saveTask(
            currentTask.getId(),
            TaskPayloadBuilder.save().withTaskId(currentTask.getId()).withVariable(variableName, variableValue).build()
        );
    }

    @When("the status of the task is $taskStatus")
    public void checkTaskStatus(Task.TaskStatus taskStatus) throws Exception {
        taskRuntimeBundleSteps.checkTaskStatus(currentTask.getId(), taskStatus);
        taskQuerySteps.checkTaskStatus(currentTask.getId(), taskStatus);

        switch (taskStatus) {
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
        Collection<? extends Task> tasks = taskQuerySteps.getAllTasks().getContent();
        assertThat(tasks).extracting("id").doesNotContain(currentTask.getId());
    }

    @Then("tasks of $processName cannot be seen by user")
    public void cannotSeeTasksOfDefinition(String processName) throws Exception {
        Collection<? extends Task> tasks = taskQuerySteps.getAllTasks().getContent();
        assertThat(tasks).extracting("processDefinitionId").doesNotContain(processDefinitionKeyMatcher(processName));
    }

    @Then("the status of the process and the task is changed to completed")
    public void verifyProcessAndTasksStatus() throws Exception {
        processQuerySteps.checkProcessInstanceStatus(
            processInstance.getId(),
            ProcessInstance.ProcessInstanceStatus.COMPLETED
        );
        auditSteps.checkProcessInstanceTaskEvent(
            processInstance.getId(),
            currentTask.getId(),
            TaskRuntimeEvent.TaskEvents.TASK_COMPLETED
        );
        //the process instance disappears once it is completed
        processRuntimeBundleSteps.checkProcessInstanceNotFound(processInstance.getId());
    }

    @Then("the status of the process is changed to completed")
    public void verifyProcessStatusCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.COMPLETED);
        auditSteps.checkProcessInstanceEvent(
            processId,
            ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED,
            AUDIT_STEP_TIMEOUT
        );
    }

    @Then("the status of the process is changed to suspended")
    public void verifyProcessStatusSuspended() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.SUSPENDED);
        auditSteps.checkProcessInstanceEvent(
            processId,
            ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED,
            AUDIT_STEP_TIMEOUT
        );
    }

    @Then("the status of the process is changed to running")
    public void verifyProcessStatusResumed() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.RUNNING);
        auditSteps.checkProcessInstanceEvent(
            processId,
            ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED,
            AUDIT_STEP_TIMEOUT
        );
    }

    @Then("a variable was created with name $variableName")
    @When("a variable was created with name $variableName")
    public void verifyVariableCreated(String variableName) throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        processQuerySteps.checkProcessInstanceHasVariable(processId, variableName);
        auditSteps.checkProcessInstanceVariableEvent(
            processId,
            variableName,
            VariableEvent.VariableEvents.VARIABLE_CREATED
        );
    }

    @When("the user deletes the process")
    public void deleteCurrentProcessInstance() throws Exception {
        processRuntimeBundleSteps.deleteProcessInstance(processInstance.getId());
    }

    @When("the admin deletes the process")
    public void adminDeleteCurrentProcessInstance() throws Exception {
        processRuntimeAdminSteps.deleteProcessInstance(processInstance.getId());
    }

    @When("the user suspends the process instance")
    public void suspendProcessInstance() throws Exception {
        processRuntimeBundleSteps.suspendProcessInstance(processInstance.getId());
    }

    @Then("the process instance is deleted")
    public void verifyProcessInstanceIsDeleted() throws Exception {
        //TODO change to DELETED status and PROCESS_DELETED event when RB is ready
        processRuntimeBundleSteps.checkProcessInstanceNotFound(processInstance.getId());
        processQuerySteps.checkProcessInstanceStatus(
            processInstance.getId(),
            ProcessInstance.ProcessInstanceStatus.CANCELLED
        );
        auditSteps.checkProcessInstanceEvent(
            processInstance.getId(),
            ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED
        );
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

    @When("query the process diagram")
    public void queryProcessInstanceDiagram() {
        processInstanceDiagram = processQuerySteps.getProcessInstanceDiagram(processInstance.getId());
    }

    @When("query the process diagram admin endpoint")
    public void queryProcessInstanceDiagramAdmin() {
        processInstanceAdminDiagram = processQueryAdminSteps.getProcessInstanceDiagram(processInstance.getId());
    }

    @Then("query the process diagram admin endpoint is unauthorized")
    public void queryProcessInstanceDiagramAdminUnauthorized() {
        try {
            processInstanceAdminDiagram = processQueryAdminSteps.getProcessInstanceDiagram(processInstance.getId());
        } catch (FeignException expected) {
            assertThat(expected.status()).isEqualTo(403);
            return;
        }

        throw new AssertionError("fail");
    }

    @Then("the query diagram is shown in admin endpoint")
    public void checkQueryProcessInstanceDiagramAdmin() throws Exception {
        processQueryAdminSteps.checkProcessInstanceDiagram(processInstanceAdminDiagram);
    }

    @Then("no query diagram is shown in admin endpoint")
    public void checkQueryProcessInstanceNoDiagramAdmin() throws Exception {
        processQueryAdminSteps.checkProcessInstanceNoDiagram(processInstanceAdminDiagram);
    }

    @Then("the query diagram is shown")
    public void checkQueryProcessInstanceDiagram() throws Exception {
        processQuerySteps.checkProcessInstanceDiagram(processInstanceDiagram);
    }

    @Then("no query diagram is shown")
    public void checkQueryProcessInstanceNoDiagram() throws Exception {
        processQuerySteps.checkProcessInstanceNoDiagram(processInstanceDiagram);
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

    @Then("the user is able to resume the process instance")
    public void activateProcessInstance() {
        processRuntimeBundleSteps.resumeProcessInstance(processInstance.getId());
    }

    @Then("the user can get events for process with variables instances in admin endpoint")
    public void checkIfEventsFromProcessesWithVariablesArePresentAdmin() {
        assertThat(processInstance).isNotNull();
        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> cloudRuntimeEvents = auditAdminSteps.getEventsAdmin().getContent();
                assertThat(cloudRuntimeEvents)
                    .extracting(CloudRuntimeEvent::getProcessInstanceId, CloudRuntimeEvent::getProcessDefinitionKey)
                    .contains(tuple(processInstance.getId(), "ProcessWithVariables"));
            });
    }

    @Then("the user can query process with variables instances in admin endpoints")
    public void checkIfProcessWithVariablesArePresentQueryAdmin() {
        assertThat(
            checkProcessInstances(
                processQueryAdminSteps.getAllProcessInstancesAdmin(),
                processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
            )
        )
            .isNotEmpty();
    }

    @Then("the user can get process with variables instances in admin endpoint")
    public void checkIfProcessWithVariablesArePresentAdmin() {
        assertThat(
            checkProcessInstances(
                processRuntimeAdminSteps.getProcessInstances(),
                processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
            )
        )
            .isNotEmpty();
    }

    @Then("the task from $processName is $status and it is called $taskName")
    public void checkTaskFromProcessInstance(String processName, Task.TaskStatus status, String taskName) {
        List<ProcessInstance> processInstancesList = new ArrayList<>(
            processRuntimeBundleSteps.getAllProcessInstances()
        );
        assertThat(processInstancesList)
            .extracting("processDefinitionKey")
            .contains(processDefinitionKeyMatcher(processName));

        //filter the list
        processInstancesList =
            processInstancesList
                .stream()
                .filter(p -> p.getProcessDefinitionKey().equals(processDefinitionKeyMatcher(processName)))
                .collect(Collectors.toList());
        assertThat(processInstancesList.size()).isEqualTo(1);

        List<Task> tasksList = new ArrayList<>(
            processRuntimeBundleSteps.getTaskByProcessInstanceId(processInstancesList.get(0).getId())
        );

        assertThat(tasksList).isNotEmpty();
        currentTask = tasksList.get(0);
        assertThat(currentTask.getStatus()).isEqualTo(status);
        assertThat(currentTask.getName()).isEqualTo(taskName);
    }

    @When("the user gets the process definitions")
    public void getProcessDefinitions() {
        Collection<ProcessDefinition> processDefinitionsRuntimeBundle = processRuntimeBundleSteps.getProcessDefinitions();
        Collection<ProcessDefinition> processDefinitionsQuery = processQuerySteps.getProcessDefinitions().getContent();

        Serenity.setSessionVariable("processDefinitionsRuntimeBundle").to(processDefinitionsRuntimeBundle);
        Serenity.setSessionVariable("processDefinitionsQuery").to(processDefinitionsQuery);
    }

    @Then("all the process definitions are present")
    public void checkProcessDefinitions() {
        Collection<ProcessDefinition> processDefinitionsRuntimeBundle = Serenity.sessionVariableCalled(
            "processDefinitionsRuntimeBundle"
        );
        Collection<ProcessDefinition> processDefinitionsQuery = Serenity.sessionVariableCalled(
            "processDefinitionsQuery"
        );
        assertThat(processDefinitionsRuntimeBundle).extracting("key").containsAll(processDefinitionKeys.values());
        assertThat(processDefinitionsQuery).extracting("key").containsAll(processDefinitionKeys.values());
    }

    @Then("the $processName definition has the $field field with value $value")
    public void checkIfFieldIsPresentAndHasValue(String processName, String field, String value) {
        ProcessDefinition processDefinition = processRuntimeBundleSteps.getProcessDefinitionByKey(
            processDefinitionKeyMatcher(processName)
        );
        assertThat(processDefinition).extracting(field).isEqualTo(value);
    }

    @Then("The user gets all the process definitions in admin endpoint")
    public void checkCanGetProcessDefinitionsAsAdmin() {
        PagedModel<CloudProcessDefinition> processDefinitions = processQueryAdminSteps.getAllProcessDefinitions();
        assertThat(processDefinitions.getContent())
            .isNotNull()
            .extracting(CloudProcessDefinition::getName)
            .contains("single-task", "Process with variables", "ConnectorProcess");
    }

    @Then("the process instance is updated")
    public void checkIfTaskUpdated() {
        auditSteps.checkProcessInstanceUpdatedEvent(processInstance.getId());
    }

    @When("the user updates the name of the process instance to $newProcessName")
    public void setTaskName(String newProcessName) {
        processInstance = processRuntimeBundleSteps.setProcessName(processInstance.getId(), newProcessName);
    }

    @Then("the process has the name $newProcessName")
    public void checkProccessInstanceName(String newProcessName) {
        assertThat(processRuntimeBundleSteps.getProcessInstanceById(processInstance.getId()).getName())
            .isEqualTo(newProcessName);

        // propagation my take some time to reach query
        await()
            .untilAsserted(() ->
                assertThat(processQuerySteps.getProcessInstance(processInstance.getId()).getName())
                    .isEqualTo(newProcessName)
            );
    }

    @When("the user set a process instance name $myProcessInstanceName and starts the process $processName")
    public void startProcessWithProcessInstanceName(String myProcessInstanceName, String processName) {
        processInstance =
            processRuntimeBundleSteps.startProcessWithProcessInstanceName(
                processDefinitionKeyMatcher(processName),
                myProcessInstanceName
            );
    }

    @Then("verify the process instance name is $myProcessInstanceName")
    public void verifyTheProcessInstanceNameIsTheOneSupplied(String myProcessInstanceName) {
        processRuntimeBundleSteps.checkProcessInstanceName(processInstance.getId(), myProcessInstanceName);
        processQuerySteps.checkProcessInstanceName(processInstance.getId(), myProcessInstanceName);
    }

    @Then("the task has the completion fields set")
    public void verifyTheCorrectCompletionFieldsAreSet() {
        await()
            .untilAsserted(() -> {
                Task queriedTask = taskQuerySteps.getTaskById(currentTask.getId());

                assertThat(queriedTask.getCompletedDate()).isNotNull();
                assertThat(queriedTask.getDuration()).isNotNull();

                List<CloudRuntimeEvent> taskCompletedEvents = auditSteps
                    .getEventsByEntityId(currentTask.getId())
                    .stream()
                    .filter(cloudRuntimeEvent ->
                        cloudRuntimeEvent.getEventType().equals(TaskRuntimeEvent.TaskEvents.TASK_COMPLETED)
                    )
                    .collect(Collectors.toList());

                assertThat(taskCompletedEvents.get(0).getEventType())
                    .isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_COMPLETED);
                assertThat(queriedTask.getCompletedDate().getTime())
                    .isEqualTo(taskCompletedEvents.get(0).getTimestamp());
                assertThat(queriedTask.getDuration())
                    .isEqualTo(taskCompletedEvents.get(0).getTimestamp() - queriedTask.getCreatedDate().getTime());
            });
    }

    @Then("the generated events have sequence number set")
    public void verifyEventSequenceNumberIsSet() {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        Collection<CloudRuntimeEvent> generatedEvents = auditSteps.getEventsByEntityId(processId);
        List<Integer> sequenceNumbers = generatedEvents
            .stream()
            .map(CloudRuntimeEvent::getSequenceNumber)
            .collect(Collectors.toList());
        for (int i = 0; i <= generatedEvents.size() - 1; i++) {
            assertThat(sequenceNumbers).contains(i);
        }
    }

    @Then("the generated events have the same message id")
    public void verifyEventMessageIdIsSet() {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> generatedEvents = auditSteps.getEventsByProcessInstanceId(processId);

                CloudRuntimeEvent cloudRuntimeEvent = generatedEvents.stream().findFirst().orElse(null);
                assertThat(cloudRuntimeEvent).isNotNull();
                generatedEvents.forEach(event ->
                    assertThat(event.getMessageId()).isEqualTo(cloudRuntimeEvent.getMessageId())
                );
            });
    }

    @Then("the process instance can be queried using LIKE operator")
    public void queryProcessByNameNameWithLikeOperator() {
        PagedModel<CloudProcessInstance> retrievedProcesses = processQuerySteps.getProcessInstancesByName(
            processInstance.getName().substring(0, 2)
        );
        for (ProcessInstance process : retrievedProcesses) {
            assertThat(process.getName()).contains(processInstance.getName().substring(0, 2));
        }
    }

    @Then("the user is able to delete all process instances in query service")
    public void deleteAllProcessInstancesQuery() {
        assertThat(processQueryAdminSteps.getAllProcessInstancesAdmin()).isNotEmpty();
        processQueryAdminSteps.deleteProcessInstances();
        assertThat(processQueryAdminSteps.getAllProcessInstancesAdmin()).isEmpty();
    }

    @Then("the user is able to delete all events in audit service")
    public void deleteAllEventsAudit() {
        assertThat(auditAdminSteps.getEventsAdmin()).isNotEmpty();
        auditAdminSteps.deleteEvents();
        assertThat(auditAdminSteps.getEventsAdmin()).isEmpty();
    }
}
