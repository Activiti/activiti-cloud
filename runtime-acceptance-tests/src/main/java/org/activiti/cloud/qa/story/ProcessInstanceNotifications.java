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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.steps.notifications.NotificationsSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.shared.model.AuthToken;
import org.activiti.cloud.acc.shared.rest.TokenHolder;
import org.assertj.core.util.Arrays;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.ReplayProcessor;

public class ProcessInstanceNotifications {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    
    @Steps
    private ProcessQuerySteps processQuerySteps;
    
    @Steps
    private NotificationsSteps notificationsSteps;
    
    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private ProcessInstance processInstance;
    private ReplayProcessor<String> data;
    private Subscription subscription;
    private String processInstanceId;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
    }
    
    @When("the user starts a process $processName with PROCESS_STARTED and PROCESS_COMPLETED events subscriptions")
    public void startProcess(String processName) throws IOException, InterruptedException {

        AuthToken authToken = TokenHolder.getAuthToken();
        
        String query = "subscription($serviceName: String!, $processDefinitionKey: String!) {" +
                      "  engineEvents(serviceName: $serviceName, processDefinitionKey: $processDefinitionKey ) {" +
                      "    PROCESS_STARTED {" +
                      "      processDefinitionKey " +
                      "      entity {" +
                      "        status" +
                      "      }" +
                      "    }" +
                      "    PROCESS_COMPLETED {" +
                      "      processDefinitionKey " +
                      "      entity {" +
                      "        status" +
                      "      }" +
                      "    }" +
                      "  }" +
                      "}";
            
        Map<String, Object> variables = Map.of("serviceName", notificationsSteps.getRuntimeBundleServiceName(),
                                               "processDefinitionKey",processDefinitionKeyMatcher(processName));
                                            

        Consumer<Subscription> action = startProcessAction(processName);
        
        data = notificationsSteps.subscribe(authToken.getAccess_token(), query, variables, action);

    }

    @When("the user starts a process $processName with SIGNAL_RECEIVED subscription")
    public void startProcessWithSignalSubscription(String processName) throws IOException, InterruptedException {

        AuthToken authToken = TokenHolder.getAuthToken();
         
        String query = "subscription($serviceName: String!) {" +
                        "  engineEvents(serviceName: $serviceName ) {" +
                        "    SIGNAL_RECEIVED {" +
                        "      entity {" +
                        "        elementId" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}";

        Map<String, Object> variables = Map.of("serviceName", notificationsSteps.getRuntimeBundleServiceName());

        Consumer<Subscription> action = startProcessAction(processName);
        
        data = notificationsSteps.subscribe(authToken.getAccess_token(), query, variables, action);
    }    
    
    private void checkProcessCreated() {
        assertThat(processInstance).isNotNull();
        processInstanceId = processInstance.getId();
    }

    @Then("the status of the process is completed")
    public void verifyProcessCompleted() throws Exception {
        try {
            processQuerySteps.checkProcessInstanceStatus(processInstanceId,
                                                         ProcessInstance.ProcessInstanceStatus.COMPLETED);
        } finally {
            // signal to stop receiving notifications 
            subscription.cancel();
        }   
    }
    
    @Then("PROCESS_STARTED and PROCESS_COMPLETED notifications are received")
    public void verifyNotifications() throws Exception {
        String startProcessMessage = "{\"payload\":{\"data\":{\"engineEvents\":{\"PROCESS_STARTED\":[{\"processDefinitionKey\":\"ConnectorProcess\",\"entity\":{\"status\":\"RUNNING\"}}],\"PROCESS_COMPLETED\":null}}},\"id\":\"1\",\"type\":\"data\"}";
        String completeProcessMessage = "{\"payload\":{\"data\":{\"engineEvents\":{\"PROCESS_STARTED\":null,\"PROCESS_COMPLETED\":[{\"processDefinitionKey\":\"ConnectorProcess\",\"entity\":{\"status\":\"COMPLETED\"}}]}}},\"id\":\"1\",\"type\":\"data\"}";
        
        notificationsSteps.verifyData(data, startProcessMessage, completeProcessMessage);
    }
    
    @SuppressWarnings("serial")
    @Then("SIGNAL_RECEIVED notification with $elementId signal event is received")
    public void verifySignalReceivedEventNotifications(String elementId) throws Exception {
        
        Map<String, Object> payload = new ObjectMap() {{
            put("payload", new ObjectMap() {{
                put("data", new ObjectMap() {{
                    put("engineEvents", new ObjectMap() {{
                        put("SIGNAL_RECEIVED", Arrays.array( new ObjectMap() {{
                            put("entity", Map.of("elementId", elementId));
                        }}));
                    }});
                }});
            }});
            put("id","1");
            put("type", "data");
        }};

        String expected =  objectMapper.writeValueAsString(payload);
        
        notificationsSteps.verifyData(data, expected);
    }    
    
    private Consumer<Subscription> startProcessAction(String processName) {
        return (s) -> {
            try {
                processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName),true);
            } catch (IOException e) {   
                s.cancel();
            }
    
            checkProcessCreated();
            
            subscription = s;
        };
    }
    
    @SuppressWarnings("serial")
    class ObjectMap extends LinkedHashMap<String, Object> {
    }
}
