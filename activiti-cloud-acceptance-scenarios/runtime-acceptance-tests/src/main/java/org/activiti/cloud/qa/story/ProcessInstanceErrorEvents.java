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

import static org.activiti.api.process.model.events.BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.BPMNError;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.task.model.CloudTask;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProcessInstanceErrorEvents {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private AuditSteps auditSteps;

    private ProcessInstance processInstance;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts a process with error events called $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName), false);
        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("the user can see a task '$taskName' with a status $status")
    public void verifyTaskFromProcessInstance(String taskName, Task.TaskStatus status) {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                Collection<CloudTask> tasks = processRuntimeBundleSteps.getTaskByProcessInstanceId(processId);

                assertThat(tasks).isNotEmpty().extracting("status", "name").containsExactly(tuple(status, taskName));
            });
    }

    @Then("the user deletes the process with error events")
    public void deleteCurrentProcessInstance() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processRuntimeBundleSteps.deleteProcessInstance(processId);
    }

    @Then("error events are emitted for the process")
    public void verifyErrorEventsForProcesses() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

                assertThat(events)
                    .filteredOn(CloudBPMNErrorReceivedEvent.class::isInstance)
                    .isNotEmpty()
                    .extracting(
                        CloudRuntimeEvent::getEventType,
                        CloudRuntimeEvent::getProcessDefinitionId,
                        CloudRuntimeEvent::getProcessInstanceId,
                        CloudRuntimeEvent::getProcessDefinitionKey,
                        CloudRuntimeEvent::getBusinessKey,
                        event -> bpmnError(event).getProcessDefinitionId(),
                        event -> bpmnError(event).getProcessInstanceId(),
                        event -> bpmnError(event).getErrorCode(),
                        event -> bpmnError(event).getErrorId(),
                        event -> bpmnError(event).getActivityType(),
                        event -> bpmnError(event).getActivityName()
                    )
                    .containsExactly(
                        tuple(
                            ERROR_RECEIVED,
                            processInstance.getProcessDefinitionId(),
                            processInstance.getId(),
                            processInstance.getProcessDefinitionKey(),
                            processInstance.getBusinessKey(),
                            processInstance.getProcessDefinitionId(),
                            processInstance.getId(),
                            "123",
                            "errorId",
                            null,
                            null
                        )
                    );
            });
    }

    private BPMNError bpmnError(CloudRuntimeEvent<?, ?> event) {
        return CloudBPMNErrorReceivedEvent.class.cast(event).getEntity();
    }
}
