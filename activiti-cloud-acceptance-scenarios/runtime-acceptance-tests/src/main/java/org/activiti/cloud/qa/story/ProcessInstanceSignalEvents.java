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

import static org.activiti.api.process.model.events.BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProcessInstanceSignalEvents {

    @Steps
    private ProcessRuntimeBundleSteps runtimeBundleSteps;

    @Steps
    private ProcessRuntimeAdminSteps runtimeBundleAdminSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;

    private CloudProcessInstance processInstanceCatchSignal;
    private CloudProcessInstance processInstanceBoundarySignal;
    private CloudProcessInstance processInstanceThrowSignal;

    @When("services are started")
    public void checkServicesStatus() {
        runtimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts a process with intermediate catch signal")
    public void startSignalCatchProcess() {
        processInstanceCatchSignal = runtimeBundleSteps.startProcess("SignalCatchEventProcess");
        assertThat(processInstanceCatchSignal).isNotNull();
    }

    @Then("the task '$taskName' is created")
    public void checkTaskIsCreated(String taskName) {
        List<Task> tasks = new ArrayList<>(
            runtimeBundleSteps.getTaskByProcessInstanceId(processInstanceBoundarySignal.getId())
        );
        assertThat(tasks).isNotEmpty();

        Task currentTask = tasks.get(0);
        assertThat(currentTask).isNotNull();
        assertThat(currentTask.getName()).isEqualTo(taskName);
    }

    @When("the user starts a process with a boundary signal")
    public void startBoundarySignalProcess() {
        processInstanceBoundarySignal = runtimeBundleSteps.startProcess("ProcessWithBoundarySignal");
        assertThat(processInstanceBoundarySignal).isNotNull();
    }

    @When("the user starts a process with intermediate throw signal")
    public void startSignalThrowProcess() {
        processInstanceThrowSignal = runtimeBundleSteps.startProcess("SignalThrowEventProcess");
        assertThat(processInstanceThrowSignal).isNotNull();
    }

    @Then("the process throwing a signal is completed")
    public void sheckSignalThrowProcessInstance() throws Exception {
        processQuerySteps.checkProcessInstanceStatus(
            processInstanceThrowSignal.getId(),
            ProcessInstance.ProcessInstanceStatus.COMPLETED
        );
    }

    @Then("the process catching a signal is completed")
    public void sheckSignalCatchProcessInstance() throws Exception {
        processQuerySteps.checkProcessInstanceStatus(
            processInstanceCatchSignal.getId(),
            ProcessInstance.ProcessInstanceStatus.COMPLETED
        );
    }

    @Then("the SIGNAL_RECEIVED event was catched up by intermediateCatchEvent process")
    public void sheckSignalReceivedEvent() throws Exception {
        checkSignalEventReceivedByProcess(processInstanceCatchSignal);
    }

    @Then("the SIGNAL_RECEIVED event was catched up by boundary signal process")
    public void sheckBoundarySignalReceivedEvent() throws Exception {
        checkSignalEventReceivedByProcess(processInstanceBoundarySignal);
    }

    @Then("query number of processes with processDefinitionKey $processDefinitionKey")
    public void checkProcessCount(String processDefinitionKey) throws Exception {
        List<CloudProcessInstance> processes = getProcessesByProcessDefinitionKey(processDefinitionKey);
        Serenity.setSessionVariable("checkCnt").to(processes.size());
    }

    @Then("check number of processes with processDefinitionKey $processDefinitionKey increased")
    public void checkProcessCountIncreased(String processDefinitionKey) throws Exception {
        Integer checkCnt = Serenity.sessionVariableCalled("checkCnt");
        await()
            .untilAsserted(() -> {
                List<CloudProcessInstance> processes = getProcessesByProcessDefinitionKey(processDefinitionKey);
                assertThat(processes).isNotEmpty();
                assertThat(processes.size()).isGreaterThan(checkCnt);
            });
    }

    @When("the admin deletes boundary signal process")
    public void deleteBoundarySignalProcesses() throws Exception {
        runtimeBundleAdminSteps.deleteProcessInstance(processInstanceBoundarySignal.getId());
    }

    @Then("boundary signal process is deleted")
    public void verifyProcessInstanceIsDeleted() throws Exception {
        runtimeBundleSteps.checkProcessInstanceNotFound(processInstanceBoundarySignal.getId());
    }

    public List<CloudProcessInstance> getProcessesByProcessDefinitionKey(String processDefinitionKey) throws Exception {
        return processQueryAdminSteps
            .getProcessInstancesByProcessDefinitionKey(processDefinitionKey)
            .getContent()
            .stream()
            .collect(Collectors.toList());
    }

    public void checkSignalEventReceivedByProcess(CloudProcessInstance process) throws Exception {
        assertThat(process).isNotNull();

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> receivedEvents = auditSteps.getEventsByProcessInstanceIdAndEventType(
                    process.getId(),
                    "SIGNAL_RECEIVED"
                );

                assertThat(receivedEvents)
                    .isNotEmpty()
                    .extracting(
                        CloudRuntimeEvent::getEventType,
                        CloudRuntimeEvent::getProcessInstanceId,
                        CloudRuntimeEvent::getProcessDefinitionKey
                    )
                    .contains(tuple(SIGNAL_RECEIVED, process.getId(), process.getProcessDefinitionKey()));
            });
    }
}
