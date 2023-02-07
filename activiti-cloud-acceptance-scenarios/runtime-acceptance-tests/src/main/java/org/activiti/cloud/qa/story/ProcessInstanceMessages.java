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

import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestInternalServerErrorIsThrownBy;
import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestNotFoundErrorIsThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProcessInstanceMessages {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;

    private ProcessInstance processInstance;

    @Given("messages: generated unique sessionVariable called $variableName")
    public void generateUniqueBusinessId(String variableName) {
        Serenity.setSessionVariable(variableName).to(UUID.randomUUID().toString());
    }

    @Given("messages: session timeout of $timeoutSeconds seconds")
    public void setSessionTimeoutSeconds(long timeoutSeconds) {
        Serenity.setSessionVariable("timeoutSeconds").to(timeoutSeconds);
    }

    @When("messages: services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When(
        "messages: the user sends a start message named $messageName with businessKey value of $businessKey session variable"
    )
    public void startMessage(String messageName, String businessKey) throws IOException, InterruptedException {
        String variableValue = Serenity.sessionVariableCalled(businessKey);

        StartMessagePayload payload = MessagePayloadBuilder.start(messageName).withBusinessKey(variableValue).build();

        processInstance = processRuntimeBundleSteps.message(payload);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then(
        "messages: the user sends a message named $messageName with correlationKey value of $correlationKey session variable"
    )
    public void receiveMessage(String messageName, String correlationKey) throws IOException, InterruptedException {
        String variableValue = Serenity.sessionVariableCalled(correlationKey);
        ReceiveMessagePayload payload = MessagePayloadBuilder
            .receive(messageName)
            .withCorrelationKey(variableValue)
            .build();

        processRuntimeBundleSteps.message(payload);
    }

    @Then(
        "messages: the user gets not found error when sends a message named $messageName with nonexisting correlationKey"
    )
    public void receiveMessageWithNonexisitingCorrelationkey(String messageName)
        throws IOException, InterruptedException {
        ReceiveMessagePayload payload = MessagePayloadBuilder
            .receive(messageName)
            .withCorrelationKey("nonexistingkey")
            .build();
        assertThatRestNotFoundErrorIsThrownBy(() -> processRuntimeBundleSteps.message(payload))
            .withMessageContaining(
                "Message subscription name '" + messageName + "' with correlation key 'nonexistingkey' not found."
            );
    }

    @Then(
        "messages: the user gets internal server error when starting a process with message named $messageName and duplicate correlationKey $correlationKey"
    )
    public void startMessageWithDuplicateCorrelationkey(String messageName, String correlationKey)
        throws IOException, InterruptedException {
        String variableValue = Serenity.sessionVariableCalled(correlationKey);

        StartMessagePayload payload = MessagePayloadBuilder.start(messageName).withBusinessKey(variableValue).build();

        assertThatRestInternalServerErrorIsThrownBy(() -> processRuntimeBundleSteps.message(payload))
            .withMessageContaining(
                "Duplicate message subscription 'boundaryMessage' with correlation key '" + variableValue + "'"
            );
    }

    @Then("messages: $eventType event is emitted for the message '$messageName'")
    public void verifyTimerScheduleEventsEmitted(String eventType, String messageName) throws Exception {
        long timeoutSeconds = sessionTimeoutSeconds();

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceIdAndEventType(
                    processInstanceId,
                    eventType
                );
                assertThat(events)
                    .isNotEmpty()
                    .extracting("eventType", "processInstanceId", "entity.messagePayload.name")
                    .contains(tuple(BPMNMessageEvent.MessageEvents.valueOf(eventType), processInstanceId, messageName));
            });
    }

    @Then("messages: the process with message events is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }

    @Then(
        "messages: the process with definition key of '$processDefinitionKey' having businessKey value of '$sessionVariable' session variable has status '$status'"
    )
    public void verifyProcessInstanceStatus(String processDefinitionKey, String sessionVariable, String status)
        throws Exception {
        long timeoutSeconds = sessionTimeoutSeconds();

        String businessKey = Serenity.sessionVariableCalled(sessionVariable);

        await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Collection<CloudProcessInstance> result = processQueryAdminSteps
                    .getProcessInstancesByProcessDefinitionKey(processDefinitionKey)
                    .getContent()
                    .stream()
                    .filter(it -> businessKey.equals(it.getBusinessKey()))
                    .collect(Collectors.toList());
                assertThat(result).isNotEmpty().extracting("status").contains(ProcessInstanceStatus.valueOf(status));
            });
    }

    @Then(
        "messages: $eventType event is emitted for the message '$messageName' for process definition key '$processDefinitionKey' having businessKey value of '$sessionVariable' session variable"
    )
    public void verifyMessageEventIsEmitted(
        String eventType,
        String messageName,
        String processDefinitionKey,
        String sessionVariable
    ) throws Exception {
        long timeoutSeconds = sessionTimeoutSeconds();

        String businessKey = Serenity.sessionVariableCalled(sessionVariable);

        await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps
                    .getEventsByProcessDefinitionKey(processDefinitionKey)
                    .stream()
                    .filter(event -> businessKey.equals(event.getBusinessKey()))
                    .filter(event -> eventType.equals(event.getEventType().name()))
                    .collect(Collectors.toList());
                assertThat(events).isNotEmpty().extracting("entity.messagePayload.name").contains(messageName);
            });
    }

    private long sessionTimeoutSeconds() {
        long timeoutSeconds = Serenity.sessionVariableCalled("timeoutSeconds");

        if (timeoutSeconds < 0) {
            timeoutSeconds = 0;
        }

        return timeoutSeconds;
    }
}
