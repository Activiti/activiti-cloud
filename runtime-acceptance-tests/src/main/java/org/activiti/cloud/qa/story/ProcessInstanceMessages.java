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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ProcessInstanceMessages {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    
    @Steps
    private ProcessQuerySteps processQuerySteps;
    
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
    
    @When("messages: the user sends a start message named $messageName with businessKey value of $businessKey session variable")
    public void startMessage(String messageName, String businessKey) throws IOException, InterruptedException {
        String variableValue = Serenity.sessionVariableCalled(businessKey);

        StartMessagePayload payload = MessagePayloadBuilder.start(messageName)
                                                           .withBusinessKey(variableValue)
                                                           .build();

        processInstance = processRuntimeBundleSteps.message(payload);
        
        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("messages: the user sends a message named $messageName with correlationKey value of $correlationKey")
    public void receiveMessage(String messageName, String correlationKey) throws IOException, InterruptedException {      
        String variableValue = Serenity.sessionVariableCalled(correlationKey);
        ReceiveMessagePayload payload = MessagePayloadBuilder.receive(messageName)
                                                             .withCorrelationKey(variableValue)
                                                             .build();

        processRuntimeBundleSteps.message(payload);
    }

    @Then("messages: $eventType event is emitted for the message '$messageName'")
    public void verifyTimerScheduleEventsEmitted(String eventType,
                                                 String messageName) throws Exception {
        long timeoutSeconds = sessionTimeoutSeconds();
        
        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");
        
        await()
               .atMost(timeoutSeconds, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceIdAndEventType(processInstanceId,
                                                                                                              eventType);
                   assertThat(events).isNotEmpty()
                                     .extracting("eventType",
                                                 "processInstanceId",
                                                 "entity.messagePayload.name")
                                     .contains(tuple(BPMNMessageEvent.MessageEvents.valueOf(eventType),
                                                     processInstanceId,
                                                     messageName));
               });
    }
    
    @Then("messages: the process with message events is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        
        processQuerySteps.checkProcessInstanceStatus(processId,
                                                     ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }
    
    private long sessionTimeoutSeconds() {
        long timeoutSeconds = Serenity.sessionVariableCalled("timeoutSeconds");
        
        if (timeoutSeconds  < 0) {
            timeoutSeconds = 0;
        }
        
        return timeoutSeconds;

    }
    
}
