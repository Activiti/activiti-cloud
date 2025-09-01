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

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.PagedModel;

public class ProcessInstanceTimers {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;

    private ProcessInstance processInstance;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts a process with timer events called $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName), false);
        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("TIMER_SCHEDULED events are emitted for the timer '$timerId' and timeout $timeoutSeconds seconds")
    public void verifyTimerScheduleEventsEmitted(String timerId, long timeoutSeconds) throws Exception {
        if (timeoutSeconds < 0) {
            timeoutSeconds = 0;
        }
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        auditSteps.checkProcessInstanceTimerScheduledEvents(processInstanceId, timerId, timeoutSeconds);
    }

    @Then("TIMER_SCHEDULED boundary events are emitted for the timer '$timerId' and timeout $timeoutSeconds seconds")
    public void verifyTimerEventsEmitted(String timerId, long timeoutSeconds) throws Exception {
        if (timeoutSeconds < 0) {
            timeoutSeconds = 0;
        }

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessAndEntityId(
                    processInstanceId,
                    timerId
                );
                assertThat(events)
                    .isNotEmpty()
                    .extracting("eventType", "entityId", "processInstanceId")
                    .contains(tuple(BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED, timerId, processInstanceId));
            });
    }

    @Then("TIMER_EXECUTED events are emitted for the timer '$timerId' and timeout $timeoutSeconds seconds")
    public void verifyTimerExecutedEventsEmitted(String timerId, long timeoutSeconds) throws Exception {
        if (timeoutSeconds < 0) {
            timeoutSeconds = 0;
        }
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        auditSteps.checkProcessInstanceTimerExecutedEvents(processInstanceId, timerId, timeoutSeconds);
    }

    @Then("the process with timer events is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }

    @Then("the admin query returns $number processes called $processName with timeout $timeoutSeconds seconds")
    public void checkProcessByProcessDefintionKey(long number, String processName, long timeoutSeconds)
        throws IOException, InterruptedException {
        if (timeoutSeconds < 0) {
            timeoutSeconds = 0;
        }

        await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                PagedModel<CloudProcessInstance> processInstances = processQueryAdminSteps.getProcessInstancesByProcessDefinitionKey(
                    processDefinitionKeyMatcher(processName)
                );

                assertThat(processInstances).isNotEmpty();
                assertThat(processInstances.getContent()).isNotNull();
                assertThat(processInstances.getContent().size()).isEqualTo(number);
            });
    }

    @Then("timer events are emitted for processes called $processName")
    public void verifyTimerEventsForProcesses(String processName) throws Exception {
        String processDefinitionKey = processDefinitionKeyMatcher(processName);

        Collection<CloudRuntimeEvent> events = auditSteps
            .getEventsByEntityId("theStart")
            .stream()
            .filter(event -> event.getProcessDefinitionId().startsWith(processDefinitionKey))
            .collect(Collectors.toList());

        assertThat(events)
            .isNotEmpty()
            .extracting("eventType")
            .contains(
                BPMNTimerEvent.TimerEvents.TIMER_SCHEDULED,
                BPMNTimerEvent.TimerEvents.TIMER_FIRED,
                BPMNTimerEvent.TimerEvents.TIMER_EXECUTED,
                BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                BPMNTimerEvent.TimerEvents.TIMER_FIRED,
                BPMNTimerEvent.TimerEvents.TIMER_EXECUTED,
                BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED
            );
    }
}
