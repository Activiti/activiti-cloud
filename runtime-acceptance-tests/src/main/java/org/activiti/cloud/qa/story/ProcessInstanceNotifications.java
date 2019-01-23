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

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.steps.notifications.NotificationsSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.shared.model.AuthToken;
import org.activiti.cloud.acc.shared.rest.TokenHolder;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import reactor.core.publisher.ReplayProcessor;

public class ProcessInstanceNotifications {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    
    @Steps
    private ProcessQuerySteps processQuerySteps;
    
    @Steps
    private NotificationsSteps notificationsSteps;
    
    private ProcessInstance processInstance;
    private ReplayProcessor<String> data;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
    }
    
    @When("the user starts a process with notifications called $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {

        AuthToken authToken = TokenHolder.getAuthToken();
         
        data = notificationsSteps.subscribe(authToken.getAccess_token());
        
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName),true);

        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
        checkProcessCreated();
    }

    private void checkProcessCreated() {
        assertThat(processInstance).isNotNull();
        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("the status of the process is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId,
                ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }
    
    @Then("notifications are received")
    public void verifyNotifications() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        notificationsSteps.verifyData(data);
    }
    

}
