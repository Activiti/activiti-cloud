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

import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestNotFoundErrorIsThrownBy;
import static org.activiti.cloud.acc.core.helper.Filters.checkEvents;
import static org.activiti.cloud.acc.core.helper.Filters.checkProcessInstances;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeys;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.withTasks;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.audit.admin.AuditAdminSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class SecurityPoliciesActions {

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

    private Task currentTask;

    @Then("the user cannot start the process with variables")
    public void startProcess() throws Exception {
        assertThatRestNotFoundErrorIsThrownBy(() ->
                processRuntimeBundleSteps.startProcess(processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))
            )
            .withMessageContaining("Unable to find process definition for the given id:'ProcessWithVariables'");
    }

    @Then("the user can get simple process instances")
    public void checkIfSimpleProcessInstancesArePresent() {
        assertThat(
            checkProcessInstances(
                processRuntimeBundleSteps.getAllProcessInstances(),
                processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE")
            )
        )
            .isNotEmpty();
    }

    @Then("the user can get process with variables instances")
    public void checkIfProcessWithVariablesArePresent() {
        assertThat(
            checkProcessInstances(
                processRuntimeBundleSteps.getAllProcessInstances(),
                processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
            )
        )
            .isNotEmpty();
    }

    @Then("the user can query simple process instances")
    public void checkIfSimpleProcessInstancesArePresentQuery() {
        assertThat(
            checkProcessInstances(
                processQuerySteps.getAllProcessInstances(),
                processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE")
            )
        )
            .isNotEmpty();
    }

    @Then("the user can get events for simple process instances")
    public void checkIfEventsFromSimpleProcessesArePresent() {
        assertThat(checkEvents(auditSteps.getAllEvents(), processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE")))
            .isNotEmpty();
    }

    @Then("the user can get events for process with variables instances")
    public void checkIfEventsFromProcessesWithVariablesArePresent() {
        assertThat(checkEvents(auditSteps.getAllEvents(), processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")))
            .isNotEmpty();
    }

    @Then("the user cannot get events for process with variables instances")
    public void checkIfEventsFromProcessesWithVariablesAreNotPresent() {
        assertThat(checkEvents(auditSteps.getAllEvents(), processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")))
            .isEmpty();
    }

    @Then("the user cannot get process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresent() {
        assertThat(
            checkProcessInstances(
                processRuntimeBundleSteps.getAllProcessInstances(),
                processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
            )
        )
            .isEmpty();
    }

    @Then("the user cannot query process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresentQuery() {
        assertThat(
            checkProcessInstances(
                processQuerySteps.getAllProcessInstances(),
                processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
            )
        )
            .isEmpty();
    }

    @Then("the user can query process with variables instances")
    public void checkIfProcessWithVariablesArePresentQuery() {
        assertThat(
            checkProcessInstances(
                processQuerySteps.getAllProcessInstances(),
                processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
            )
        )
            .isNotEmpty();
    }

    @Then("the user can get tasks")
    public void checkIfTaskArePresent() {
        assertThat(taskRuntimeBundleSteps.getAllTasks()).isNotNull();
    }

    @Then("the user can query tasks")
    public void checkIfTaskArePresentQuery() {
        assertThat(taskQuerySteps.getAllTasks().getContent()).isNotNull();
    }

    @When("the user starts an instance of the process called $processName")
    public void startProcess(String processName) {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName));

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
        }

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
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

    @Then("the user can get events for process with variables instances in admin endpoint")
    public void checkIfEventsFromProcessesWithVariablesArePresentAdmin() {
        //TODO some refactoring after fixing the behavior of the /admin/v1/events?search=entityId:UUID endpoint
        Collection<CloudRuntimeEvent> filteredCollection = checkEvents(
            auditAdminSteps.getEventsByEntityIdAdmin(Serenity.sessionVariableCalled("processInstanceId")),
            processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")
        );
        assertThat(filteredCollection).isNotEmpty();
        assertThat(((ProcessInstanceImpl) filteredCollection.iterator().next().getEntity()).getProcessDefinitionKey())
            .isEqualTo(processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"));
    }
}
